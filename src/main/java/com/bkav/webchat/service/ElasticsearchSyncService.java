//package com.bkav.webchat.service;
//
//import com.bkav.webchat.entity.Message;
//import com.bkav.webchat.entity.MessageDocument;
//import com.bkav.webchat.repository.MessageSearchRepository;
//import com.bkav.webchat.service.Impl.MessageServiceImpl;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.event.TransactionalEventListener;
//
//import java.time.ZoneId;
//
//@Service
//public class ElasticsearchSyncService {
//
//    @Autowired
//    private MessageSearchRepository searchRepository;
//
//    /**
//     * Hàm này sẽ tự động chạy KHI SỰ KIỆN MessageSyncEvent được phát ra
//     * và CHỈ SAU KHI transaction của Postgres (lưu message) đã COMMIT thành công.
//     */
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handleMessageSyncEvent(MessageServiceImpl.MessageSyncEvent event) {
//        Message message = event.getMessage();
//
//        try {
//            if ("DELETE".equals(event.getAction())) {
//                // Thực hiện XÓA khỏi Elasticsearch
//                searchRepository.deleteById(message.getMessageId());
//                System.out.println("Đã xóa messageId " + message.getMessageId() + " khỏi ES.");
//            } else {
//                // Thực hiện TẠO MỚI/CẬP NHẬT vào Elasticsearch
//                MessageDocument doc = toDocument(message);
//                searchRepository.save(doc);
//                System.out.println("Đã đồng bộ messageId " + message.getMessageId() + " vào ES.");
//            }
//        } catch (Exception e) {
//
//            System.err.println("Lỗi đồng bộ ES cho messageId " + message.getMessageId() + ": " + e.getMessage());
//        }
//    }
//
//    // chuyển đổi Message  -> MessageDocument
//    private MessageDocument toDocument(Message message) {
//        MessageDocument doc = new MessageDocument();
//        doc.setMessageId(message.getMessageId());
//        doc.setContent(message.getContent());
//        doc.setMessageType(message.getMessageType());
//        doc.setConversationId(message.getConversation().getConversationId());
//        doc.setSenderId(message.getSender().getAccountId());
//        doc.setCreatedAt(message.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
//        return doc;
//    }
//
//}
