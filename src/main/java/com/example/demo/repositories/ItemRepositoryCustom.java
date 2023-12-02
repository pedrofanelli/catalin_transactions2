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

	void checkNameDuplicate(String name);
	
	void addItem(String name, LocalDate creationDate);

	void addItemNoRollback(String name, LocalDate creationDate);
	
    void addLogs();

    void showLogs();

    
}
