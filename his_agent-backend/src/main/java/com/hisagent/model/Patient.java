package com.hisagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 患者实体
 */
@Entity
@Table(name = "patients")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Patient {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String idCard;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String gender;

    @Column
    private Integer age;

    @Column(length = 500)
    private String address;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
