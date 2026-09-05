package com.voyage.expense.repository;

import com.voyage.expense.domain.Expense;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("select distinct e from Expense e left join fetch e.splits where e.tripId = :tripId order by e.spentOn desc, e.id desc")
    List<Expense> findByTripIdWithSplits(@Param("tripId") Long tripId);

    @Query("select e from Expense e left join fetch e.splits where e.id = :id")
    Optional<Expense> findByIdWithSplits(@Param("id") Long id);
}
