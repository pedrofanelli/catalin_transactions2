package com.example.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Integer>, LogRepositoryCustom {

}
