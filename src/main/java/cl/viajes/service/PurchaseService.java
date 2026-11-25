package cl.viajes.service;

import cl.viajes.dto.*;
import cl.viajes.model.Product;
import cl.viajes.model.Sale;
import cl.viajes.model.Ticket;
import cl.viajes.repository.ProductRepository;
import cl.viajes.repository.SaleRepository;
import cl.viajes.repository.TicketRepository;
import cl.viajes.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class PurchaseService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final TicketRepository ticketRepository;
    private final ActivityLogService activityLog;
    private final UserRepository userRepository;

    public PurchaseService(
            ProductRepository productRepository,
            SaleRepository saleRepository,
            TicketRepository ticketRepository,
            ActivityLogService activityLog,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.ticketRepository = ticketRepository;
        this.activityLog = activityLog;
        this.userRepository = userRepository;
    }

    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest request) {
        return createPurchase(request, null);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listMine(Long userId) {
        return saleRepository.findOrderSummariesByUser(userId);
    }

    @Transactional(readOnly = true)
    public List<Integer> occupiedSeats(String slug, String scheduleId) {
        Product product = productRepository.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada"));
        for (Map<?, ?> candidate : scheduleMaps(product)) {
            if (scheduleId.equals(stringValue(candidate.get("id")))) {
                LocalDate date = parseDate(stringValue(candidate.get("date")));
                LocalTime time = parseTime(stringValue(candidate.get("time")));
                return ticketRepository.findByProductIdAndScheduleDateAndDepartureTimeAndStatus(
                                product.getId(), date, time, "activo")
                        .stream()
                        .map(Ticket::getSeatNumber)
                        .toList();
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Salida no encontrada");
    }

    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest request, Long userId) {
        List<Ticket> tickets = new ArrayList<>();
        Map<String, Set<Integer>> reservedBySchedule = new HashMap<>();
        long total = 0;

        for (PurchaseItem item : request.items()) {
            if (item.slug() == null || item.slug().isBlank() || item.passengers() == null || item.passengers().isEmpty()) {
                throw invalid("Cada pasaje debe incluir ruta, salida y datos de sus pasajeros");
            }

            Product product = productRepository.findBySlugForUpdate(item.slug().trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> conflict("Una de las rutas ya no está disponible"));
            if (!"Activo".equals(product.getStatus())) {
                throw conflict("Una de las rutas ya no está disponible");
            }

            TripSchedule schedule = resolveSchedule(product, item);
            SeatRange seatRange = seatRange(product, item);
            String scheduleKey = product.getId() + "|" + schedule.date() + "|" + schedule.time();
            Set<Integer> occupied = reservedBySchedule.computeIfAbsent(scheduleKey, ignored -> new HashSet<>());
            for (Ticket sold : ticketRepository.findByProductIdAndScheduleDateAndDepartureTimeAndStatus(
                    product.getId(), schedule.date(), schedule.time(), "activo")) {
                occupied.add(sold.getSeatNumber());
            }

            List<Integer> seats = seatsForPassengers(seatRange, occupied, item.passengers(), schedule.capacity());
            if (seats == null) {
                throw conflict("Uno de los asientos seleccionados ya no está disponible");
            }

            long originalPrice = Math.round(product.getPrice() * (double) seatRange.priceFactor() / 100);
            int discount = product.getDiscountPercent();
            long pricePaid = Math.round(originalPrice * (100 - discount) / 100.0);
            for (int index = 0; index < item.passengers().size(); index++) {
                PassengerRequest passenger = item.passengers().get(index);
                String name = passenger.name() == null ? "" : passenger.name().trim();
                String rut = passenger.rut() == null ? "" : passenger.rut().replaceAll("[^0-9kK]", "");
                if (name.isBlank() || name.length() > 150 || !rut.matches("\\d{7,8}[0-9kK]")) {
                    throw invalid("Ingresa nombre y RUT válido para cada pasajero");
                }

                Ticket ticket = new Ticket();
                ticket.setTicketCode(newCode());
                ticket.setProductId(product.getId());
                ticket.setTripTitle(product.getTitle());
                String[] route = product.getTitle().split(" - ", 2);
                ticket.setOrigin(route.length == 2 ? route[0].trim() : null);
                ticket.setDestination(product.getDestination());
                ticket.setScheduleDate(schedule.date());
                ticket.setDepartureTime(schedule.time());
                ticket.setPlatform(schedule.platform());
                ticket.setSeatNumber(seats.get(index));
                ticket.setPassengerName(name);
                ticket.setPassengerRut(rut);
                ticket.setPassengerEmail(passenger.email() == null ? null : passenger.email().trim());
                ticket.setPassengerPhone(passenger.telephone() == null ? null : passenger.telephone().trim());
                ticket.setSeatClass(product.getDistanceKm() >= 400 || "Salón Cama".equals(product.getType())
                        ? ("salon".equalsIgnoreCase(item.seatClass()) ? "Salón Cama" : "Semi Cama")
                        : product.getType());
                ticket.setOriginalPrice(originalPrice);
                ticket.setDiscountPercent(discount);
                ticket.setPricePaid(pricePaid);
                ticket.setDistanceKm(product.getDistanceKm());
                tickets.add(ticket);
                total += pricePaid;
            }
        }

        Sale sale = new Sale();
        sale.setSaleCode(newCode());
        sale.setTotalAmount(total);
        sale.setUserId(userId);
        sale.setBuyerName(request.buyerName() == null ? null : request.buyerName().trim());
        sale.setBuyerEmail(request.buyerEmail() == null ? null : request.buyerEmail().trim());
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                sale.setBuyerName(user.getName());
                sale.setBuyerEmail(user.getEmail());
            });
        }
        tickets.forEach(sale::addTicket);
        saleRepository.save(sale);
        String actor = tickets.get(0).getPassengerName();
        activityLog.record("compra", "Compra " + sale.getSaleCode().substring(0, 8) + ": "
                + tickets.size() + " boleto(s) por $" + String.format(Locale.forLanguageTag("es-CL"), "%,d", total), actor);

        return new PurchaseResponse(
                sale.getSaleCode(),
                total,
                tickets.stream().map(TicketResponse::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public SaleDetailsResponse getPurchase(String saleCode) {
        return saleDetails(saleCode);
    }

    @Transactional(readOnly = true)
    public SaleDetailsResponse getPurchase(String saleCode, Long userId, boolean admin) {
        Sale sale = findSale(saleCode);
        if (!admin && !Objects.equals(sale.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta compra");
        }
        return SaleDetailsResponse.from(sale, sale.getTickets().stream().map(TicketResponse::from).toList());
    }

    private SaleDetailsResponse saleDetails(String saleCode) {
        Sale sale = findSale(saleCode);
        return SaleDetailsResponse.from(
                sale,
                sale.getTickets().stream().map(TicketResponse::from).toList()
        );
    }

    private Sale findSale(String saleCode) {
        return saleRepository.findBySaleCode(saleCode.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la compra solicitada"));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listOrders() {
        return saleRepository.findAllOrderSummaries();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> searchOrders(String search, LocalDate date) {
        return saleRepository.searchOrders(search == null ? "" : search.trim().toLowerCase(Locale.ROOT),
                date == null ? null : date.atStartOfDay(), date == null ? null : date.plusDays(1).atStartOfDay());
    }

    @Transactional
    public TicketResponse updateTicket(String ticketCode, TicketUpdateRequest request, String actor) {
        String code = ticketCode.trim().toLowerCase(Locale.ROOT);
        TicketRepository.TicketLocation location = ticketRepository.findLocationByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boleto no encontrado"));
        Product product = productRepository.findByIdForUpdate(location.getProductId())
                .orElseThrow(() -> conflict("La ruta del boleto ya no existe"));
        if (!"Activo".equals(product.getStatus())) throw conflict("La ruta ya no está disponible");
        Sale sale = saleRepository.findBySaleCodeForUpdate(location.getSaleCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
        Ticket ticket = ticketRepository.findActiveTicketsBySaleIdForUpdate(sale.getId()).stream()
                .filter(item -> code.equals(item.getTicketCode()))
                .findFirst().orElseThrow(() -> conflict("El boleto ya está cancelado"));

        String name = request.passengerName().trim();
        String rut = request.passengerRut().replaceAll("[^0-9kK]", "");
        if (name.isBlank() || !rut.matches("\\d{7,8}[0-9kK]")) {
            throw invalid("Ingresa nombre y RUT válido para el pasajero");
        }
        String seatClass = ticket.getSeatClass();
        if (seatClass == null && (ticket.getDistanceKm() >= 400 || "Salón Cama".equals(product.getType()))) {
            seatClass = ticket.getSeatNumber() > 52 ? "Salón Cama" : "Semi Cama";
        }
        String classKey = "Salón Cama".equals(seatClass) ? "salon" : "semi";
        PurchaseItem item = new PurchaseItem(product.getSlug(), request.scheduleId(), false, null, null,
                classKey, List.of(new PassengerRequest(name, rut, request.seatNumber())));
        TripSchedule schedule = resolveSchedule(product, item);
        SeatRange range = seatRange(product, item);
        List<Ticket> occupied = ticketRepository.findOccupiedForUpdate(product.getId(), schedule.date(), schedule.time());
        Set<Integer> taken = new HashSet<>();
        occupied.stream().filter(other -> !other.getId().equals(ticket.getId())).forEach(other -> taken.add(other.getSeatNumber()));
        List<Integer> seats = seatsForPassengers(range, taken, item.passengers(), schedule.capacity());
        if (seats == null) throw conflict("El asiento seleccionado ya no está disponible");
        ticket.setScheduleDate(schedule.date());
        ticket.setDepartureTime(schedule.time());
        ticket.setPlatform(schedule.platform());
        ticket.setSeatNumber(seats.get(0));
        ticket.setPassengerName(name);
        ticket.setPassengerRut(rut);
        ticket.setPassengerEmail(request.passengerEmail() == null ? null : request.passengerEmail().trim());
        ticket.setPassengerPhone(request.passengerPhone() == null ? null : request.passengerPhone().trim());
        ticket.setSeatClass(seatClass);
        activityLog.record("reprogramacion", "Boleto " + code.substring(0, Math.min(8, code.length()))
                + " actualizado: " + schedule.date() + " " + schedule.time() + ", asiento " + seats.get(0), actor);
        return TicketResponse.from(ticket);
    }

    private TripSchedule resolveSchedule(Product product, PurchaseItem item) {
        LocalDate today = LocalDate.now();
        List<Map<?, ?>> schedules = scheduleMaps(product);

        if (item.customSchedule()) {
            boolean hasFutureSchedule = schedules.stream()
                    .map(candidate -> stringValue(candidate.get("date")))
                    .anyMatch(date -> date.compareTo(today.toString()) >= 0);
            if (hasFutureSchedule) {
                throw conflict("Esta ruta ya tiene salidas publicadas. Selecciona una de ellas");
            }
            LocalDate date = parseDate(item.scheduleDate());
            LocalTime time = parseTime(item.departureTime());
            if (date.isBefore(today)) {
                throw invalid("Selecciona una fecha y hora de viaje válidas");
            }
            return new TripSchedule(date, time, "Por asignar", null);
        }

        if (item.scheduleId() == null || item.scheduleId().isBlank()) {
            throw invalid("Cada pasaje debe incluir ruta y salida");
        }
        for (Map<?, ?> candidate : schedules) {
            if (item.scheduleId().equals(stringValue(candidate.get("id")))) {
                LocalDate date = parseDate(stringValue(candidate.get("date")));
                LocalTime time = parseTime(stringValue(candidate.get("time")));
                if (date.isBefore(today)) {
                    throw conflict("Una de las salidas seleccionadas ya no está disponible");
                }
                return new TripSchedule(
                        date,
                        time,
                        stringValue(candidate.get("platform")),
                        product.getDistanceKm() >= 400 ? 64 : integerValue(candidate.get("capacity"))
                );
            }
        }
        throw conflict("Una de las salidas seleccionadas ya no está disponible");
    }

    private List<Map<?, ?>> scheduleMaps(Product product) {
        List<Map<?, ?>> schedules = new ArrayList<>();
        for (Object schedule : product.getSchedules()) {
            if (schedule instanceof Map<?, ?> map) {
                schedules.add(map);
            }
        }
        return schedules;
    }

    private SeatRange seatRange(Product product, PurchaseItem item) {
        if (product.getDistanceKm() >= 400 || "Salón Cama".equals(product.getType())) {
            if ("salon".equalsIgnoreCase(item.seatClass())) {
                return new SeatRange(List.of(71, 72, 74, 75, 76, 78, 79, 80, 82, 83, 84, 86), 125);
            }
            if (item.seatClass() != null && !item.seatClass().isBlank()
                    && !"semi".equalsIgnoreCase(item.seatClass())) {
                throw invalid("Selecciona Salón Cama o Semi Cama para este bus de dos pisos");
            }
            return new SeatRange(IntStream.rangeClosed(1, 52).boxed().toList(), 100);
        }
        if ("Semi Cama".equals(product.getType())) {
            return new SeatRange(IntStream.rangeClosed(1, 44).boxed().toList(), 100);
        }
        return new SeatRange(IntStream.rangeClosed(1, 56).boxed().toList(), 100);
    }

    private List<Integer> seatsForPassengers(
            SeatRange range,
            Set<Integer> occupied,
            List<PassengerRequest> passengers,
            Integer capacity
    ) {
        List<Integer> selected = new ArrayList<>(passengers.size());
        if (capacity != null && occupied.size() + passengers.size() > capacity) {
            return null;
        }
        boolean hasSelection = passengers.stream().anyMatch(passenger -> passenger.seatNumber() != null);
        if (!hasSelection) {
            List<Integer> available = new ArrayList<>(passengers.size());
            for (int seat : range.seatNumbers()) {
                if (!occupied.contains(seat) && available.size() < passengers.size()) {
                    occupied.add(seat);
                    available.add(seat);
                }
            }
            return available.size() == passengers.size() ? available : null;
        }
        Set<Integer> unique = new HashSet<>();
        for (PassengerRequest passenger : passengers) {
            Integer seat = passenger.seatNumber();
            if (seat == null || !range.seatNumbers().contains(seat)
                    || occupied.contains(seat) || !unique.add(seat)) {
                return null;
            }
            occupied.add(seat);
            selected.add(seat);
        }
        return selected;
    }

    private LocalDate parseDate(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new DateTimeParseException("Invalid ISO date", value == null ? "" : value, 0);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid("Selecciona una fecha y hora de viaje válidas");
        }
    }

    private LocalTime parseTime(String value) {
        try {
            if (value == null || !value.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) {
                throw new DateTimeParseException("Invalid time", value == null ? "" : value, 0);
            }
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException exception) {
            throw invalid("Selecciona una fecha y hora de viaje válidas");
        }
    }

    private Integer integerValue(Object value) {
        if (!(value instanceof Number number) || number.doubleValue() != number.intValue()) {
            return null;
        }
        return number.intValue();
    }

    private String stringValue(Object value) {
        return value instanceof String string ? string : "";
    }

    private String newCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record TripSchedule(LocalDate date, LocalTime time, String platform, Integer capacity) {
    }

    private record SeatRange(List<Integer> seatNumbers, int priceFactor) {
    }
}
