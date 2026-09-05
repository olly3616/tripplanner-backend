package com.voyage.poll.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private Vote(Long pollId, Long optionId, Long userId) {
        this.pollId = pollId;
        this.optionId = optionId;
        this.userId = userId;
    }

    public static Vote of(Long pollId, Long optionId, Long userId) {
        return new Vote(pollId, optionId, userId);
    }
}
