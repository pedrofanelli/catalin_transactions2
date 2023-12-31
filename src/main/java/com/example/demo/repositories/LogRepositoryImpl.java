package com.example.demo.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.*;

/**
 * When we inject a LogRepository bean, Spring Data will detect that LogRepository implements 
 * LogRepositoryCustom and will look up a class called LogRepositoryImpl to act as a custom repository 
 * implementation. Consequently, the methods of the injected LogRepository bean will have the same behavior 
 * as the methods of the LogRepositoryImpl class.
 * 
 * @author peter
 *
 */
public class LogRepositoryImpl implements LogRepositoryCustom {

	@Autowired
    private LogRepository logRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        logRepository.save(new Log(message));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void addSeparateLogsNotSupported() {
        logRepository.save(new Log("check from not supported 1"));
        if (true) throw new RuntimeException();
        logRepository.save(new Log("check from not supported 2"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void addSeparateLogsSupports() {
        logRepository.save(new Log("check from supports 1"));
        if (true) throw new RuntimeException();
        logRepository.save(new Log("check from supports 2"));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void showLogs() {
        System.out.println("Current log:");
        logRepository.findAll().forEach(System.out::println);
    }
}
