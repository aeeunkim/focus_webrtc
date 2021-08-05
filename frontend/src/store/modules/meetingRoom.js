import Participant from './js/participant.js'
import kurentoUtils from 'kurento-utils'

export default {
  namespaced: true,
  // state
  state: () => ({
    ws: null,
    participants: null,
    serverMessage: null,
    myName: null,
  }),
  // mutations
  mutations: {
    WS_INIT(state, url){
      state.ws = new WebSocket(url)
    },
    WS_ONMESSAGE(state, parsedMessage) {
      state.serverMessage = parsedMessage
    },
    SET_MY_NAME(state, myName) {
      state.myName = myName
    },
    ADD_PARTICIPANT(state, {name, participant}) {
      if (state.participants===null) {
        state.participants = {}
      }
      state.participants[name] = participant
      // 디버깅
      console.log('participant added', state.participants)
    },
    DISPOSE_PARTICIPANT(state, participantName) {
      delete state.participants[participantName]
    }
  },
  // actions
  actions: {
    // meetingRoom view 생성시 웹소켓 생성 action
    wsInit(context, url) {
      context.commit('WS_INIT', url)
      context.state.ws.onmessage = function(message) {
        let parsedMessage = JSON.parse(message.data)
        console.info('Received message: ' + message.data)
        context.commit('WS_ONMESSAGE', parsedMessage)
      }
    },
    // 웹소켓으로 메시지 발신 action
    sendMessage(context, message) {
      var jsonMessage = JSON.stringify(message)
      console.log('Sending message: ' + jsonMessage)
      context.state.ws.send(jsonMessage)
    },
    // user 이름 설정 action
    setMyName(context, myName) {
      context.commit('SET_MY_NAME', myName)
    },
    // onExistingParticipants 메시지에 대한 반응
    onExistingParticipants(context, message) {
      console.log('onExistingParticipants start')
      // 브라우저 사용자의 Participant 오브젝트 정의
      let participant = new Participant(context.state.myName)
      var video = participant.getVideoElement()

      // WebRtcPeer의 옵션 설정
      var constraints = {
        audio: true,
        video: {
          mandatory: {
            maxWidth : 320,
            maxFrameRate : 15,
            minFrameRate : 15,
          }
        }
      }
      var options = {
        localVideo: video,
        mediaConstraints: constraints,
        onicecandidate: participant.onIceCandidate.bind(participant),
      }

      // user participant 오브젝트의 WebRtcPeer 선언
      participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(
        options,
        function(error) {
          if (error) {
            //debugging
            console.log(participant, video)
            return console.error(error)
          }
          // this -> kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly
          // generateOffer: 
          this.generateOffer(participant.offerToReceiveVideo.bind(participant))
        }
      )
      
      // state에 user participant 오브젝트 추가
      const myName = context.state.myName
      context.commit('ADD_PARTICIPANT', { name: myName, participant })
      // state에 방에 있던 participant들 오브젝트 추가
      message.data.forEach(function(sender) {
        console.log('forEach문 sender: ' + sender)
        context.dispatch('receiveVideo', sender)
        })
      console.log('onExistingParticipants end')
    },
    // 다른 참가자 participant 비디오 받기
    receiveVideo(context, sender) {
      let participant = new Participant(sender)
      var video = participant.getVideoElement()
      var options = {
        remoteVideo: video,
        onicecandidate: participant.onIceCandidate.bind(participant)
      }
      // 참가자의 rtcPeer 설정
      participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(
        options,
        function(error) {
          if(error) {
            return console.error(error);
          }
          // this -> kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly
          this.generateOffer (participant.offerToReceiveVideo.bind(participant));
      })
      // 참가자를 state의 참가자 목록에 추가
      context.commit('ADD_PARTICIPANT', { name:sender, participant })
    },
    // 방에 참여해있는 상태에서 새로운 참가자가 들어왔을 때
    onNewParticipant(context, request) {
      console.log('onNewParticipant' + request.name)
      context.dispatch('receiveVideo', request.name)
    },
    // participant 객체에서 삭제 메서드를 사용했을 때
    onParticipantLeft(context, request) {
      console.log(context.state.myName)
      console.log('Participant' + request.name + 'left')
      var participant = context.state.participants[request.name]
      participant.dispose()
    },
    // participant.dispose에서 오는 요청
    disposeParticipant(context, participantName) {
      context.commit('DISPOSE_PARTICIPANT', participantName)
    },
    receiveVideoResponse(context, result) {
      context.state.participants[result.name].rtcPeer.processAnswer(result.sdpAnswer, function (error) {
        if (error) return console.error (error)
      })
    }
  },
  getters: {
    
  },
};
