package cl.viajes.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_code", nullable = false, unique = true, length = 32)
    private String saleCode;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "buyer_name", length = 150)
    private String buyerName;

    @Column(name = "buyer_email", length = 190)
    private String buyerEmail;

    @Column(nullable = false, length = 20)
    private String status = "pagada";

    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<Ticket> tickets = new ArrayList<>();

    public Long getId() { return id; }
    public String getSaleCode() { return saleCode; }
    public void setSaleCode(String saleCode) { this.saleCode = saleCode; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Ticket> getTickets() { return tickets; }
    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
        ticket.setSale(this);
    }
}
