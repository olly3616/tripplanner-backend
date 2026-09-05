package com.voyage.poll.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "polls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poll extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "multiple_choice", nullable = false)
    private boolean multipleChoice;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "poll_id", nullable = false)
    private List<PollOption> options = new ArrayList<>();

    private Poll(Long tripId, Long createdBy, String title, boolean multipleChoice, boolean anonymous,
                 Instant closesAt, List<PollOption> options) {
        this.tripId = tripId;
        this.createdBy = createdBy;
        this.title = title;
        this.multipleChoice = multipleChoice;
        this.anonymous = anonymous;
        this.closesAt = closesAt;
        this.options = new ArrayList<>(options);
    }

    public static Poll create(Long tripId, Long createdBy, String title, boolean multipleChoice,
                              boolean anonymous, Instant closesAt, List<PollOption> options) {
        return new Poll(tripId, createdBy, title, multipleChoice, anonymous, closesAt, options);
    }

    public boolean isClosed(Instant now) {
        return !now.isBefore(closesAt);
    }
}
