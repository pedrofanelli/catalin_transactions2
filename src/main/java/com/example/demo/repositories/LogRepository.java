package com.example.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.models.Log;

public interface LogRepository extends JpaRepository<Log, Integer>, LogRepositoryCustom {

}
