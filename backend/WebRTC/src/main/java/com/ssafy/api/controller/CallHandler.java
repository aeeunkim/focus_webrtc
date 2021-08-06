/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ssafy.api.controller;

import java.io.IOException;

import com.ssafy.api.service.PresentationManager;
import com.ssafy.api.service.RoomManager;
import com.ssafy.common.util.Presentation;
import com.ssafy.common.util.Room;
import com.ssafy.common.util.UserRegistry;
import com.ssafy.common.util.UserSession;
import org.kurento.client.IceCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;


/**
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.3.1
 */
public class CallHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

  private static final Gson gson = new GsonBuilder().create();

  @Autowired
  private RoomManager roomManager;

  @Autowired
  private UserRegistry registry;

  @Autowired
  private PresentationManager presentationManager;

  /*
  TODO 여러 사람이 쓰는 거니까 전역변수 쓰면 안되고 final 써야 할것같음
       수정 필요
   */
  private Presentation presentation;
  private String sdpOffer="";

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    final JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    final UserSession user = registry.getBySession(session);

    if (user != null) {
      log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
    } else {
      log.debug("Incoming message from new user: {}", jsonMessage);
    }

    switch (jsonMessage.get("id").getAsString()) {
      case "joinRoom":
        joinRoom(jsonMessage, session);
        break;
      case "receiveVideoFrom":
        final String senderName = jsonMessage.get("sender").getAsString();
        final UserSession sender = registry.getByName(senderName);
        sdpOffer = jsonMessage.get("sdpOffer").getAsString();
        user.receiveVideoFrom(sender, sdpOffer);
        break;
      case "leaveRoom":
        leaveRoom(user);
        break;
      case "onIceCandidate":
        JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

        if (user != null) {
          IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
              candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(cand, jsonMessage.get("name").getAsString());
        }
        break;
      case "presenterSet":{
        log.trace("presenterSet");
        presenterSet(jsonMessage);
        break;
      }
      case "startPresentation": {
    	  final String presenterName = jsonMessage.get("presenter").getAsString();
    	  final UserSession presenter = registry.getByName(presenterName);
    	  for(UserSession audience : registry.getUsersByName().values()) {
    		  audience.linkImageOverlayPipeline(presenter, presentationManager.getImageOverlayFilter());
    	  }
      }
      case "start":{
        log.trace("start");
        start();
        break;
      }
      case "stop":{
        log.trace("stop");
        stop();
        break;
      }
      case "prev":{
        log.trace("prev");
        prev(sdpOffer);
        break;
      }
      case "next":{
        log.trace("next");
        next(sdpOffer);

        break;
      }
      case "full":{
        log.trace("full toggle");
        full();
        break;
      }
      case "moveImage":{
        final String location = jsonMessage.get("location").getAsString();
        log.trace("move image");
        presentationManager.changeImageLocation(location);
        break;
      }
      default:
        break;
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    UserSession user = registry.removeBySession(session);
    roomManager.getRoom(user.getRoomName()).leave(user);
  }

  private void joinRoom(JsonObject params, WebSocketSession session) throws IOException {
    final String roomName = params.get("room").getAsString();
    final String name = params.get("name").getAsString();
    log.info("PARTICIPANT {}: trying to join room {}", name, roomName);

    Room room = roomManager.getRoom(roomName);
    final UserSession user = room.join(name, session);
    registry.register(user);
  }

  private void leaveRoom(UserSession user) throws IOException {
    final Room room = roomManager.getRoom(user.getRoomName());
    room.leave(user);
    if (room.getParticipants().isEmpty()) {
      roomManager.removeRoom(room);
    }
  }

  private void presenterSet(JsonObject params) throws IOException {
    //TODO presentationSession이랑 presentationManager.getPresenter() 꼬인거 해결해야함
    String presenter = params.get("presenter").getAsString();
    boolean isPresenter = params.get("isPresenter").getAsBoolean();
    UserSession presenterSession = registry.getByName(presenter);
    Room room = roomManager.getRoom(presenterSession.getRoomName());

    presentation = presentationManager.getPresentation(presenter, room, presenterSession);
    presentationManager.setPresenter(isPresenter);
    registry.register(presentationManager.getPresenter());
    log.info("[presentationSet] presentation: {}", presentation);

  }


  private void start() {
    presentationManager.start();
  }

  private void stop() {
    //presentationManager.stop();
  }

  private void prev(String sdpOffer) {
    presentationManager.prev(sdpOffer);
  }

  private void next(String sdpOffer) {
    presentationManager.next(sdpOffer);
  }

  private void full(){
    presentationManager.full();
  }
}
