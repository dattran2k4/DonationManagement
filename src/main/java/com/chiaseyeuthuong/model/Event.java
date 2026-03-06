package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.EEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_amount")
    private BigDecimal currentAmount;

    @Column(name = "target_amount")
    private BigDecimal targetAmount;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "location")
    private String location;

    @Column(name = "number_of_activities")
    private Integer numberOfActivities;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EEventStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "event")
    private List<Donation> donations = new ArrayList<>();

    @OneToMany(mappedBy = "event")
    private List<Activity> activities = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
