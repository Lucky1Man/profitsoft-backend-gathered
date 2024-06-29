package org.example.task5part1.repository;

import org.example.task5part1.document.SendMailRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SendMailRequestRepository extends
        ElasticsearchRepository<SendMailRequest, UUID>, CustomSendMailRequestRepository {
    @Query(
            """
            {
                "match": {
                    "send_status": {
                        "query": "ERROR"
                    }
                }
            }
            """
    )
    Page<SendMailRequest> getAllFailedMailRequests(Pageable pageable);

}
