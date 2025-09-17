package com.webchat.webchat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "friendship")
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_friendship")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;   // Người gửi lời mời

    @ManyToOne
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;   // Người nhận lời mời

    @Column(name = "status", nullable = false)
    private String status;    // PENDING | ACCEPTED | REJECTED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
