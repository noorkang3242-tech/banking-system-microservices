package com.bank.card.repository;

import com.bank.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Card> findByCardId(String cardId);

    boolean existsByCardNumber(String cardNumber);
}
