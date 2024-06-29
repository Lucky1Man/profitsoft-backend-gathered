package org.example.task5part1.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.task5part1.document.SendMailRequest;
import org.example.task5part1.repository.CustomSendMailRequestRepository;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomSendMailRequestRepositoryImpl implements CustomSendMailRequestRepository {

    private final ElasticsearchOperations operations;

    @Override
    public void updateAll(List<SendMailRequest> requests) {
        if(requests.isEmpty()) {
            return;
        }
        operations.bulkUpdate(requests.stream().map(req ->
                UpdateQuery.builder(req.getId().toString())
                        .withDocument(operations.getElasticsearchConverter().mapObject(req))
                        .build()
        ).toList(), SendMailRequest.class);
    }
}
