package demo.leonardogcsoares.webrtc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import soares.leonardo.com.greta.signaling.Connected;
import soares.leonardo.com.greta.signaling.Greta;
import soares.leonardo.com.greta.signaling.Subscriber;

public class LocalPeerConnectionActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private PeerConnectionFactory peerConnectionFactory;

    private PeerConnection peerConnection;

    private DataChannel mDataChannel;


    private Button callButton;
    private Button hangupButton;

    private String localMessageReceived = " ";
    private String localLastMessageReceived = " ";

    private boolean isInitiator = false;

    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "peerConnectionObserver onSignalingChange() " + signalingState.name());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "peerConnectionObserver onIceConnectionChange() " + iceConnectionState.name());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "peerConnectionObserver onIceConnectionReceivingChange(): " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "peerConnectionObserver onIceGatheringChange() " + iceGatheringState.name());
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "peerConnectionObserver onIceCandidate: " + iceCandidate.toString());

            JSONObject json = new JSONObject();
            String mes;

            try {
                json.put("type", "candidate");
                json.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                json.put("sdpMid", iceCandidate.sdpMid);
                json.put("candidate", iceCandidate.sdp);

                mes = json.toString();

                Log.d(TAG, "iceCandidateJson" + mes);
                Greta.Signaling.publish("iceChannel", mes);

            } catch (org.json.JSONException ex) {
                Log.d(TAG, ex.toString());
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "peerConnectionObserver onDataChannel()");
            mDataChannel = dataChannel;
            mDataChannel.registerObserver(dataChannelObserver);
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "peerConnectionObserver onRenegotiationNeeded()");
        }
    };

    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(TAG, "local onCreateSuccess");


            if (sessionDescription.type == SessionDescription.Type.OFFER && isInitiator) {
                peerConnection.setLocalDescription(sdpObserver, sessionDescription);
                JSONObject json = new JSONObject();
                String message;

                try {
                    json.put("type", sessionDescription.type.toString().toLowerCase());
                    json.put("sdp", sessionDescription.description);
                    message = json.toString();
//                Log.d(TAG, message);

                    /** Here we implement the signaling mecanism that will pass and retrieve the sdp message**/
                    Greta.Signaling.publish("sdpChannel", message);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (sessionDescription.type == SessionDescription.Type.ANSWER && !isInitiator) {
                peerConnection.setLocalDescription(sdpObserver, sessionDescription);

                JSONObject json = new JSONObject();
                String message;

                try {
                    json.put("type", sessionDescription.type.toString().toLowerCase());
                    Log.d(TAG, "sdpType: " + sessionDescription.type.toString().toLowerCase());
                    json.put("sdp", sessionDescription.description);
                    message = json.toString();
//                Log.d(TAG, message);

                    /** Here we implement the signaling mecanism that will pass and retrieve the sdp message**/
                    Greta.Signaling.publish("sdpChannel", message);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG, "local onSetSuccess");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d(TAG, "local onCreateFailure " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.d(TAG, "local onSetFailure " + s);
        }
    };

    DataChannel.Observer dataChannelObserver = new DataChannel.Observer() {

        @Override
        public void onBufferedAmountChange(long l) {

        }

        @Override
        public void onStateChange() {
            Log.d(TAG, "dataChannelObserver onStateChange() " + mDataChannel.state().name());
        }

        @Override
        public void onMessage(DataChannel.Buffer buffer) {
            Log.d(TAG, "dataChannelObserver onMessage()");

            if (!buffer.binary) {
                int limit = buffer.data.limit();
                byte[] datas = new byte[limit];
                buffer.data.get(datas);
                localMessageReceived = new String(datas);

            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "init MainActivity onCreate");

        Log.d(TAG, PeerConnectionFactory.initializeAndroidGlobals(getApplicationContext(), true, true, true)
                ? "Success initAndroidGlobals" : "Failed initAndroidGlobals");


        peerConnectionFactory = new PeerConnectionFactory();
        Log.d(TAG, "has yet to create local and remote peerConnection");

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        subscribeToSignalingService();


        List<PeerConnection.IceServer> iceServers = new LinkedList<>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        MediaConstraints constraints = new MediaConstraints();
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, constraints, peerConnectionObserver);

        mDataChannel = peerConnection.createDataChannel("RTCDataChannel", new DataChannel.Init());
        mDataChannel.registerObserver(dataChannelObserver);

        setSendButtonListeners();


    }

    private void subscribeToSignalingService() {
        Greta.initializeGretaConnection("someRandomToken");

        Greta.Signaling.connected(new Connected() {
            @Override
            public void onConnection() {
              Log.d(TAG, "Greta Signaling finished connecting");

                Greta.Signaling.subscribe("sdpChannel", new Subscriber() {
                    @Override
                    public void messageReceived(String message) {
                        try {
                            Log.d(TAG, "inside sdpChannel");
                            Log.d(TAG, "sdpChannel message: " + message);
                            Log.d(TAG, "sdpChannel isInitiator: " + isInitiator);

                            JSONObject jsonMessage = new JSONObject(message);
                            if (jsonMessage.getString("type").equals("offer") && !isInitiator) {

                                Log.d(TAG, "sdpChannel, Type OFFER");

                                String type = jsonMessage.getString("type");
                                String sdp = jsonMessage.getString("sdp");

                                SessionDescription sdp2 = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp);
                                peerConnection.setRemoteDescription(sdpObserver, sdp2);

                                Log.d(TAG, "createAnswer called");
                                peerConnection.createAnswer(sdpObserver, new MediaConstraints());

                            }

                            else if (jsonMessage.getString("type").equals("answer") && isInitiator) {

                                Log.d(TAG, "Initiator, Type Answer: " + isInitiator);
                                String type = jsonMessage.getString("type");
                                String sdp = jsonMessage.getString("sdp");

                                SessionDescription sdp2 = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp);
                                peerConnection.setRemoteDescription(sdpObserver, sdp2);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                Greta.Signaling.subscribe("iceChannel", new Subscriber() {
                    @Override
                    public void messageReceived(String message) {

                        Log.d(TAG, "Not initiator, iceChannel, is initiator: " + isInitiator);
                        Log.d(TAG, "messageReceived: " + message);
                        try {
                            JSONObject jsonMessage = new JSONObject(message);

                            IceCandidate candidate = new IceCandidate(jsonMessage.getString("sdpMid"),
                                    jsonMessage.getInt("sdpMLineIndex"), jsonMessage.getString("candidate"));
                            peerConnection.addIceCandidate(candidate);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void setSendButtonListeners() {

        callButton = (Button) findViewById(R.id.peerConnectionCall);
        assert callButton != null;
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "callButton clicked");
                isInitiator = true;

                callButton.setEnabled(false);
                hangupButton.setEnabled(true);

                if (peerConnection == null) {
                    List<PeerConnection.IceServer> iceServers = new LinkedList<>();
                    iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

                    MediaConstraints constraints = new MediaConstraints();
                    peerConnection = peerConnectionFactory.createPeerConnection(iceServers, constraints, peerConnectionObserver);

                    mDataChannel = peerConnection.createDataChannel("RTCDataChannel", new DataChannel.Init());
                    mDataChannel.registerObserver(dataChannelObserver);
                }

                peerConnection.createOffer(sdpObserver, new MediaConstraints());
            }
        });

        hangupButton = (Button) findViewById(R.id.peerConnectionHangup);
        assert hangupButton != null;
        hangupButton.setEnabled(false);
        hangupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "hangupButton clicked");

                isInitiator = false;

                callButton.setEnabled(true);
                hangupButton.setEnabled(false);

                if (mDataChannel != null) {
                    mDataChannel.close();
                    mDataChannel.unregisterObserver();
                }

                if (peerConnection != null)
                    peerConnection.close();
            }
        });


        Button sendMessage = (Button) findViewById(R.id.sendMessage);
        assert sendMessage != null;
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText messageET = (EditText) findViewById(R.id.peerSendMessageEditText);
                assert messageET != null;

                String message = messageET.getText().toString();
                if (!message.isEmpty()) {
                    if (mDataChannel.state() == DataChannel.State.OPEN) {
                        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                        mDataChannel.send(new DataChannel.Buffer(buffer, false));
                    }
                }
            }
        });

        Timer timer = new Timer("Timer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!localMessageReceived.equals(localLastMessageReceived)) {
                            TextView messageReceived = (TextView) findViewById(R.id.peerMessageReceived);
                            messageReceived.setText(localMessageReceived);
                            localLastMessageReceived = localMessageReceived;
                        }
                    }
                });
            }
        }, 0, 1000);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDataChannel != null) {
            mDataChannel.close();
            mDataChannel.unregisterObserver();
        }

        if (peerConnection != null)
            peerConnection.close();


    }


}
