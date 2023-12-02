package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.example.demo.configuration.*;
import com.example.demo.exceptions.DuplicateItemNameException;
import com.example.demo.repositories.ItemRepository;
import com.example.demo.repositories.LogRepository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SpringDataConfiguration.class})
public class TransactionPropagationTest {

	@Autowired
    private ItemRepository itemRepository;

    @Autowired
    private LogRepository logRepository;

    @BeforeEach    // each TEST
    public void clean() {
        itemRepository.deleteAll();
        logRepository.deleteAll();
    }
    
    @Test
    public void notSupported() {
        // executing in transaction:
        // addLogs is starting transaction, but addSeparateLogsNotSupported() suspends it
    	
    	/**
    	 *  Ejecutamos itemRepository.addLogs() que es @Transaction(REQUIRED) porque es el default.
    	 *  Eso significa que si surge una nueva transacción en principio se mantiene dentro del paraguas de la original,
    	 *  es decir, de la inicial; sino se creará una nueva transacción. En nuestro caso de addLogs(). 
    	 *  
    	 *  Dentro de este método se ejecuta logRepository.addSeparateLogsNotSupported(); es decir un método de otro bean.
    	 *  Este último tiene @Transactional(propagation = Propagation.NOT_SUPPORTED). Es decir, que si una transacción
    	 *  ya se está ejecutando (como es nuestro ejemplo), la misma se "suspenderá" y continuará ejecutandose como 
    	 *  non-transactional la que estamos parados. Por eso en el ejemplo se agrega un log al repositorio y se 
    	 *  lanza una excepción apropósito. 
    	 *  
    	 *  La excepción lanzada demuestra que NO estamos dentro de una transacción, porque si lo estuvieramos
    	 *  entonces se haría un rollback, y lo agregado con anterioridad se borraría!
    	 *  
    	 */
        assertAll(
                () -> assertThrows(RuntimeException.class, () -> itemRepository.addLogs()),
                () -> assertEquals(1, logRepository.findAll().size()),
                () -> assertEquals("check from not supported 1", logRepository.findAll().get(0).getMessage())
        );

        // no transaction - first record is added in the log even after exception!
        // metodo que imprime cada log en el repositorio
        logRepository.showLogs();
    }

    
    
    @Test
    public void supports() {
        // executing without transaction:
        // addSeparateLogsSupports is working with no transaction
    	
    	/**
    	 * En este caso no hay propagación a otros Beans. Directamente se ejecuta logRepository.addSeparateLogsSupports().
    	 * Este método tiene como propagación @Transactional(propagation = Propagation.SUPPORTS), lo que significa que,
    	 * va a formar parte de otra transacción que esté en ejecución (como fue el ejemplo anterior) y si no existiera
    	 * ninguna, funciona como non-transactional. 
    	 * 
    	 * La excepción lanzada demuestra que NO estamos dentro de una transacción, porque si lo estuvieramos
    	 * entonces se haría un rollback, y lo agregado con anterioridad se borraría!
    	 * 
    	 *  El ejemplo sería igual al anterior pero sin intervenir una transacción externa.
    	 */
        assertAll(
                () -> assertThrows(RuntimeException.class, () -> logRepository.addSeparateLogsSupports()),
                () -> assertEquals(1, logRepository.findAll().size()),
                () -> assertEquals("check from supports 1", logRepository.findAll().get(0).getMessage())
        );

        // no transaction - first record is added in the log even after exception!
        logRepository.showLogs();
    }
    
    @Test
    public void mandatory() {
        // get exception because checkNameDuplicate can be executed only in transaction
    	
    	/**
    	 * En este caso, el método checkNameDuplicate, tiene @Transactional(propagation = Propagation.MANDATORY).
    	 * Eso significa que NECESARIAMENTE debe existir una transacción en curso, sino lanza excepción.
    	 * 
    	 * En el ejemplo se lanza IllegalTransactionStateException
    	 */
    	
        IllegalTransactionStateException ex = assertThrows(IllegalTransactionStateException.class, () -> itemRepository.checkNameDuplicate("Item1"));
        assertEquals("No existing transaction found for transaction marked with propagation 'mandatory'", ex.getMessage());
    }
    /*
    @Test
    public void never() {
        itemRepository.addItem("Item1", LocalDate.of(2022, 5, 1));
        // it's safe to call showLogs from no transaction
        logRepository.showLogs();

        // but prohibited to execute from transaction
        IllegalTransactionStateException ex = assertThrows(IllegalTransactionStateException.class, () -> itemRepository.showLogs());
        assertEquals("Existing transaction found for transaction marked with propagation 'never'", ex.getMessage());
    }

    @Test
    public void requiresNew() {
        // requires new - log message is persisted in the logs even after exception
        // because it was added in the separate transaction
        itemRepository.addItem("Item1", LocalDate.of(2022, 5, 1));
        itemRepository.addItem("Item2", LocalDate.of(2022, 3, 1));
        itemRepository.addItem("Item3", LocalDate.of(2022, 1, 1));

        DuplicateItemNameException ex = assertThrows(DuplicateItemNameException.class, () -> itemRepository.addItem("Item2", LocalDate.of(2016, 3, 1)));
        assertAll(
                () -> assertEquals("Item with name Item2 already exists", ex.getMessage()),
                () -> assertEquals(4, logRepository.findAll().size()),
                () -> assertEquals(3, itemRepository.findAll().size())
        );

        System.out.println("Logs: ");
        logRepository.findAll().forEach(System.out::println);

        System.out.println("List of added items: ");
        itemRepository.findAll().forEach(System.out::println);
    }

    @Test
    public void noRollback() {
        // no rollback - log message is persisted in the logs even after exception
        // because transaction was not rolled back
        itemRepository.addItemNoRollback("Item1", LocalDate.of(2022, 5, 1));
        itemRepository.addItemNoRollback("Item2", LocalDate.of(2022, 3, 1));
        itemRepository.addItemNoRollback("Item3", LocalDate.of(2022, 1, 1));

        DuplicateItemNameException ex = assertThrows(DuplicateItemNameException.class, () -> itemRepository.addItem("Item2", LocalDate.of(2016, 3, 1)));
        assertAll(
                () -> assertEquals("Item with name Item2 already exists", ex.getMessage()),
                () -> assertEquals(4, logRepository.findAll().size()),
                () -> assertEquals(3, itemRepository.findAll().size())
        );

        System.out.println("Logs: ");
        logRepository.findAll().forEach(System.out::println);

        System.out.println("List of added items: ");
        itemRepository.findAll().forEach(System.out::println);
    }
    
    */
}
