package com.example.demo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.models.Item;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {

	Optional<Item> findByName(String name);
}
