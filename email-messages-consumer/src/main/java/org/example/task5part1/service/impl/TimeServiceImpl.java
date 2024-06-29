package org.example.task5part1.service.impl;

import org.example.task5part1.service.TimeService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TimeServiceImpl implements TimeService {
    @Override
    public Instant instantUtcNow() {
        return Instant.now(Clock.systemUTC());
    }
}
