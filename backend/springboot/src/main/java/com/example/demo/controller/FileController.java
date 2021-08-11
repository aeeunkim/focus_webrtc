package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.api.request.FileReq;
import com.example.demo.entity.Rooms;
import com.example.demo.model.response.BaseResponseBody;
import com.example.demo.model.response.RoomGetRes;
import com.example.demo.service.FileService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
@CrossOrigin
@RestController
@RequestMapping("/board")
public class FileController {
	@Autowired
	private FileService fileservice;
	
	
	@PostMapping("/down")
	    public ResponseEntity<?> downloadfile(
	    		@ModelAttribute @ApiParam(value="파일 저장", required = true) FileReq registerInfo
	    		)throws Exception {
		  	FileReq file = fileservice.saveFile(registerInfo);
			return  ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));	
	    }
	
//	@GetMapping("/")
//	@ApiOperation(value = "전체 방 보기")
//	@ApiResponses({
//			@ApiResponse(code = 200, message = "성공"),
//			@ApiResponse(code = 401, message = "인증 실패"),
//			@ApiResponse(code = 404, message = "사용자 없음"),
//			@ApiResponse(code = 409, message = "이미 존재하는 유저"),
//			@ApiResponse(code = 500, message = "서버 오류")
//	})
//	public ResponseEntity<List<RoomGetRes>> showRooms(){
//		return new ResponseEntity<List<RoomGetRes>>(u.findAll(), HttpStatus.OK);
//
//	}
	
	
	@PostMapping("/updatefile")
	@ApiOperation(value = "파일 수정") 

	 public ResponseEntity<?> updatefile(
	    		@ModelAttribute @ApiParam(value="파일 수정", required = true) FileReq registerInfo
	    		)throws Exception {
		
	//	  	FileReq file = fileservice.update(registerInfo);
			return  ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));	
	    }
}
