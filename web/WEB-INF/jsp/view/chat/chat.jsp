<%--@elvariable id="chatSessionId" type="long"--%>

<template:basic htmlTitle="Support Chat" bodyTitle="Support Chat">
    
    <jsp:attribute name="extraHeadContent">
        <link rel="stylesheet" href="<c:url value="/resource/stylesheet/chat.css" />" />
        <script src="http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.3.1/js/bootstrap.min.js"></script>
    </jsp:attribute>
    
    <jsp:body>
        <div id="chatContainer">
        	<!-- 채팅내용 -->
            <div id="chatLog">

            </div>
            
            <!-- 내용입력 -->
            <div id="messageContainer">
                <textarea id="messageArea"></textarea>
            </div>
            
            <!-- 버튼 -->
            <div id="buttonContainer">
                <button class="btn btn-primary" onclick="send();">Send</button>
                <button class="btn" onclick="disconnect();">Disconnect</button>
            </div>
        </div>
        
        <!-- 채팅 모달 -->
        <div id="modalError" class="modal hide fade">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h3>Error</h3>
            </div>
            <div class="modal-body" id="modalErrorBody">A blah error occurred.</div>
            <div class="modal-footer">
                <button class="btn btn-primary" data-dismiss="modal">OK</button>
            </div>
        </div>
        
        <script>
            var send, disconnect;
            
            $(document).ready(function() {
                var modalError     = $("#modalError");          	//에러모달창
                var modalErrorBody = $("#modalErrorBody");      	//에러모달창 내용
                var chatLog        = $("#chatLog");					//채팅내용
                var messageArea    = $("#messageArea");				//채팅입력
                var username       = '${sessionScope.username}';	//사용자명
                var otherJoined    = false;
                
                if(!("WebSocket" in window)) {
                    modalErrorBody.text('사용중인 브라우저는 채팅서비스가 지원되지 않습니다.');
                    modalError.modal('show');
                    return;
                }

                var infoMessage = function(m) {
                    chatLog.append($('<div>').addClass('informational').text(moment().format('h:mm:ss a') + ': ' + m));
                };
                
                infoMessage('채팅서버 접속중입니다...');

                var objectMessage = function(message) {
                    var log = $('<div>');
                    var date = message.timestamp == null ? '' : moment.unix(message.timestamp).format('h:mm:ss a');
                    
                    if(message.user != null) {
                        var c = message.user == username ? 'user-me' : 'user-you';
                        
                        log.append($('<span>').addClass(c).text(date+' '+message.user+'\xA0:\xA0')).append($('<span>').text(message.content));
                        
                    } else {
                        log.addClass(message.type == 'ERROR' ? 'error' : 'informational').text(date + ' ' + message.content);
                    }
                    
                    chatLog.append(log);
                };

                var server;
                try {
                    server = new WebSocket('ws://' + window.location.host + '<c:url value="/chat/${chatSessionId}" />');
                    //server.binaryType = 'arraybuffer';
                    //server.binaryType = 'blob';
                    
                } catch(error) {
                    modalErrorBody.text(error);
                    modalError.modal('show');
                    return;
                }

                // ONOPEN
                server.onopen = function(event) {
                    infoMessage('채팅서버 연결되었습니다.');
                };

                // ONCLOSE
                server.onclose = function(event) {
                    if(server != null) infoMessage('연결이 종료되었습니다.');
                    
                    server = null;
                    
                    //정상종료가 아닌경우
                    if( !event.wasClean || event.code != 1000 ) {
                        modalErrorBody.text('Code ' + event.code + ': ' + event.reason);
                        modalError.modal('show');
                        
                        console.log('Code   : ' + event.code);
                        console.log('Reason : ' + event.reason);
                    }
                };

                // ONERROR
                server.onerror = function(event) {
                    modalErrorBody.text(event.data);
                    modalError.modal('show');
                };

                // ONMESSAGE
                server.onmessage = function(event) {
            
                	console.log(event.data);
                	
                    if(typeof event.data === 'string') {
                    	
                    	var message = JSON.parse(event.data);
                    	
                    	//채팅화면에 입력
                        console.log('message => ');
                        console.log(message);
                        
                        //채팅화면에 입력
                        objectMessage(message);
                        
                        if(message.type == 'JOINED') {
                            otherJoined = true;
                            if(username != message.user){
                                infoMessage('You are now chatting with ' + message.user + '.');
                            }
                        }
                        
                    } else {
                        modalErrorBody.text('Unexpected data type [' + typeof(event.data) + '].');
                        modalError.modal('show');
                    }
                };

                // SEND
                send = function() {
                    if(server == null) {
                        modalErrorBody.text('You are not connected!');
                        modalError.modal('show');
                        
                    } else if(!otherJoined) {
                        modalErrorBody.text('다른사용자가 아직 참여하지 않았습니다.');
                        modalError.modal('show');
                    
					//입력내용 없는경우
                    } else if(messageArea.val() === '') {
                    	modalErrorBody.text('메세지를 입력해주세요');
                        modalError.modal('show');
                    
					//입력내용 있는경우
                    } else {
                        var message = {
                              timestamp : new Date()
                        	, type      : 'TEXT'
                        	, user      : username
                        	, content   : messageArea.val()
                        };
                        
                        try {
                            var json   = JSON.stringify(message);
                            
                            var length = json.length;
                            //var buffer = new ArrayBuffer(length*2);
                            //var array  = new Uint8Array(buffer);
                            
                            //for(var i = 0; i < length; i++) {
                                //array[i] = json.charCodeAt(i);
                            //}
                            
                            console.log(json);
                            console.log(typeof(json));
                            
                            server.send(json);
                            
                            messageArea.get(0).value = '';
                            
                        } catch(error) {
                            modalErrorBody.text(error);
                            modalError.modal('show');
                        }
                    }
                };

                // DISCONNECT
                disconnect = function() {
                    if(server != null) {
                        infoMessage('연결이 종료되었습니다.');
                        server.close();
                        server = null;
                    }
                };

                window.onbeforeunload = disconnect;
            });
        </script>
        
    </jsp:body>
    
</template:basic>

