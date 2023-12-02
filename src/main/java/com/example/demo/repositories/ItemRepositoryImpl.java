package com.example.demo.repositories;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ItemRepositoryImpl {

	@Autowired
    private ItemRepository itemRepository;

    @Autowired
    private LogRepository logRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void checkNameDuplicate(String name) {
        if (itemRepository.findAll().stream().map(item -> item.getName()).filter(n -> n.equals(name)).count() > 0) {
            throw new DuplicateItemNameException("Item with name " + name + " already exists");
        }
    }

    @Override
    @Transactional
    public void addItem(String name, LocalDate creationDate) {
        logRepository.log("adding item with name " + name);
        checkNameDuplicate(name);
        itemRepository.save(new Item(name, creationDate));
    }

    @Override
    @Transactional(noRollbackFor = DuplicateItemNameException.class)
    public void addItemNoRollback(String name, LocalDate creationDate) {
        logRepository.save(new Log("adding log in method with no rollback for item " + name));
        checkNameDuplicate(name);
        itemRepository.save(new Item(name, creationDate));
    }

    @Override
    @Transactional
    public void addLogs() {
        logRepository.addSeparateLogsNotSupported();
    }

    @Override
    @Transactional
    public void showLogs() {
        logRepository.showLogs();
    }
}
