package org.example.task5part1.repository;

import org.example.task5part1.document.SendMailRequest;

import java.util.List;

public interface CustomSendMailRequestRepository {
    void updateAll(List<SendMailRequest> requests);
}
