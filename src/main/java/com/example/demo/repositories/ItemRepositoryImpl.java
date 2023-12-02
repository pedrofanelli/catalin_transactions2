package com.example.demo.repositories;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exceptions.DuplicateItemNameException;
import com.example.demo.models.Item;
import com.example.demo.models.Log;

/**
 * We’ll provide an implementation class for ItemRepository next. The key part of this class name is 
 * the Impl ending. It is not connected to Spring Data and it only implements ItemRepositoryCustom. When 
 * injecting an ItemRepository bean, Spring Data will have to create a proxy class; it will detect that 
 * ItemRepository implements ItemRepositoryCustom and will look up a class called ItemRepositoryImpl to act 
 * as a custom repository implementation. Consequently, the methods of the injected ItemRepository bean will 
 * have the same behavior as the methods of the ItemRepositoryImpl class.
 * 
 * Es decir, funciona para implementar los métodos de una interfaz como es ItemRepositoryCustom, interfaz que
 * buscará implementarse cuando busque Spring crear el bean de ItemRepository. Dado que este último repo 
 * implementa ItemRepositoryCustom. 
 * 
 * @author peter
 *
 */
public class ItemRepositoryImpl implements ItemRepositoryCustom {

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
