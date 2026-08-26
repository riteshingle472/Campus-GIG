package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation,Long> {

    Optional<Conversation> findByContractId(Long contractId);
}
