package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message,Long>{
    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true " +
            "WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.isRead = false")
    int markAllAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

}
