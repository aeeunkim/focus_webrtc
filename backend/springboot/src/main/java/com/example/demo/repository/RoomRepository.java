package com.example.demo.repository;


import com.example.demo.entity.Rooms;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 유저 모델 관련 디비 쿼리 생성을 위한 JPA Query Method 인터페이스 정의.
 */
@Repository
public interface RoomRepository extends JpaRepository<Rooms, Integer> {
	Rooms findByRoomId(int roomId);
	@Transactional
	void deleteAllByRoomId(int roomId);
}