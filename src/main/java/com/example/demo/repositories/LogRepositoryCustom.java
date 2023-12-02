package com.example.demo.repositories;

/**
 * Another fragment interface
 * 
 * @author peter
 *
 */
public interface LogRepositoryCustom {

	void log(String message);

    void showLogs();

    void addSeparateLogsNotSupported();

    void addSeparateLogsSupports();
}
