# WebRTC Two-Androids Basic Tutorial
>**Using the DataChannel to communicate between two peers on two Android devices**

This tutorial is based on a simpler version of a [WebRTC Simple Android Tutorial demo that can be found here](https://github.com/leonardogcsoares/WebRTC-Android-Basic-Tutorial)

Due to its simplicity it can only create a peer connection between two phones with the app installed. It serves only as the basis of the process of two peers connecting.

In no way do I intend in this tutorial, to teach you the basics of how WebRTC works. That information can be acquire from links like these:
>**Learning about WebRTC**
- [Getting Started with WebRTC - Sam Dutton](https://www.google.com.br/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0ahUKEwjbgbOLhIPMAhXBiZAKHZ1FA5EQFggrMAA&url=http%3A%2F%2Fwww.html5rocks.com%2Fen%2Ftutorials%2Fwebrtc%2Fbasics%2F&usg=AFQjCNF-Cvvqsgt-nyHOSYclhUhG7NuCng&sig2=pVTzGWz0k51V-GlzK1LVfQ&bvm=bv.119028448,d.Y2I)
- [WebRTC in the real world: STUN, TURN and signaling] (http://www.html5rocks.com/en/tutorials/webrtc/infrastructure/)

>**Samples used as basis for this tutorial**
- [PeerConnection Sample](https://webrtc.github.io/samples/src/content/peerconnection/pc1/)
- [Generate and Transfer Data](http://webrtc.github.io/samples/src/content/datachannel/datatransfer/)

###Getting Started
Before developing Android apps that use native WebRTC you need the compiled code. [WebRTC.org](https://webrtc.org/native-code/android/) offers
a barebones guide to obtaining the compiled Java code. But a simpler and faster way to get this library is to use the shortcut offered by [io.pristine](http://mvnrepository.com/artifact/io.pristine/libjingle).
This is done by placing the following inside the `build.gradle` of the app.
```
compile 'io.pristine:libjingle:_version_@aar'
```
Where `_version_` represents the current version of the library. The current working version is `11139`. (04/09/2016)
  
###How it works

As you start the app the first steps it takes is creating the PeerConnection between the "Local" Peer and "Remote" Peer. All the steps taken are logged using the `log.d()` function to show the steps.

For communication to occur between two or more peers, we need some type of signaling system. WebRTC API doesn't offer any mechanism for signaling, but makes you implement one yourself.

For the purpose of this tutorial I have created a [Simple Signaling Mechanism that can be found here](https://github.com/leonardogcsoares/Signaling-API-for-Greta), which I will use to exchange messages between peers.


#To-Do
- [ ] Switch out `Button` for [FButton](https://github.com/hoang8f/android-flat-button), more aesthetically pleasing and might also solve call/hangup button disable issue.
- [ ] Implement `start` button that creates both the `PeerConnectionFactory` and `PeerConnection` instances.
- [ ] Fix callButton/hangupButton enable/disable logic.
- [ ] Implement onPause/onResume PeerConnection handling (connect/disconnect)
