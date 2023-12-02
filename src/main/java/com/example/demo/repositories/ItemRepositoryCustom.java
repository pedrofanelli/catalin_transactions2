package com.example.demo.repositories;

import java.time.LocalDate;

/**
 * Such an interface is known as a FRAGMENT INTERFACE, and its purpose is to extend a repository 
 * with custom functionality, which will be provided by a later implementation.
 * 
 * @author peter
 *
 */
public interface ItemRepositoryCustom {

	void addItem(String name, LocalDate creationDate);

    void checkNameDuplicate(String name);

    void addLogs();

    void showLogs();

    void addItemNoRollback(String name, LocalDate creationDate);
}
