package com.gestion.qnt.repository;

import com.gestion.qnt.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByAutorIdOrderByCreatedAtDesc(Long autorId);
    List<Ticket> findAllByOrderByCreatedAtDesc();
}
