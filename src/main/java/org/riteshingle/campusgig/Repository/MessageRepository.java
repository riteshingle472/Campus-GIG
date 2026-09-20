package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message,Long>{
    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);
}
