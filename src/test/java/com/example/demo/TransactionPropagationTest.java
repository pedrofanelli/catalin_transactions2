package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.example.demo.configuration.*;
import com.example.demo.repositories.ItemRepository;
import com.example.demo.repositories.LogRepository;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SpringDataConfiguration.class})
public class TransactionPropagationTest {

	@Autowired
    private ItemRepository itemRepository;

    @Autowired
    private LogRepository logRepository;

    @BeforeEach
    public void clean() {
        itemRepository.deleteAll();
        logRepository.deleteAll();
    }
    
    
}
