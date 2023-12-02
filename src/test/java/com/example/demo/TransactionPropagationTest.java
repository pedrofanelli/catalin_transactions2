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
    
    
    /**
     * 
     * Esta da unas vueltas pero se entiende.
     * 
     * Utilizamos el método itemRepository.addItem() para agregar un Item. Tiene un modo REQUIRED, es decir, default.
     * Primero, ejecuta logRepository.log(), para loguear el Item que se va a agregar en el futuro. Este método tiene
     * @Transactional(propagation = Propagation.REQUIRES_NEW), es decir, CREA una nueva transacción y SUSPENDE la actual.
     * Va a guardar en la base Log ese log. Al finalizar, se reanuda la transacción anterior.
     * Ejecuta un método propio checkNameDuplicate() con @Transactional(propagation = Propagation.MANDATORY). Esto es,
     * que si o si supportea una transacción actual, sino lanza excepción. Como existe una transacción activa, continua.
     * Chekea que no existe un item con el nombre que se quiere agregar y procede. Volvemos al original.
     * Finalmente, la transaccion origial guarda Item en la base. La persiste.
     * 
     * Luego, el test ejecuta logRepository.showLogs(); con @Transactional(propagation = Propagation.NEVER), es decir, que
     * se ejecuta non-transactionally, y si hubiera una activa, lanza excepción. Como no hay una activa, se ejecuta, y muestra
     * todos los logs en la base.
     * 
     * Luego en el assert, se ejecuta itemRepository.showLogs() en modo REQUIRED. Que la idea es ejecutar el mismo método de antes,
     * el problema que esta vez SI hay una transacción activa, entonces se lanza una excepcion. 
     * 
     */
    @Test
    public void never() {
        itemRepository.addItem("Item1", LocalDate.of(2022, 5, 1));
        // it's safe to call showLogs from no transaction
        logRepository.showLogs();

        // but prohibited to execute from transaction
        IllegalTransactionStateException ex = assertThrows(IllegalTransactionStateException.class, () -> itemRepository.showLogs());
        assertEquals("Existing transaction found for transaction marked with propagation 'never'", ex.getMessage());
    }
    
    /**
     * El método de agregar Item lo explicamos en el Test anterior.
     * 
     * Ahora, que sucede si se agrega uno duplicado.
     * Primero, se guarda el log que dice que se agrega el item. Luego se ejecuta el método propio checkNameDuplicate() que
     * controla la duplicación. Si existiera (que es el caso) lanza la excepción DuplicateItemNameException(). Esta fue
     * creada por nosotros! Extiende una RuntimeException.
     * 
     * Luego demuestra que hay 4 logs (porque se agrego antes el que después fracasó) y 3 items.
     * 
     * Aunque se haya lanzado una excepción que debería generar un rollback, no afecta al método que guardo el log porque 
     * es una transacción a parte!
     */
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
    
    /**
     * En este ejemplo, muy similar al anterior, sucede exactamente lo mismo, se lanza la excepción por duplicado 
     * DuplicateItemNameException PERO, si bien es una RuntimeException y debería generar un rollback, no lo hace porque
     * expresamente la excluimos. @Transactional(noRollbackFor = DuplicateItemNameException.class)
     * 
     * Entonces, lo guardado se mantiene, no hay rollback, se mantienen 4 logs. PERO el 4to item, si bien no hay rollback
     * no llega nunca a agregarse porque la excepción fue lanzada. Eso explica el resultado final.
     */
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
    
    
}
