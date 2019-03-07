# android-background-recorder

### features this app has that other apps(spy camera/secret recorder...those sorts) on market doesn't have
- continue recording whenever camera access is available.It will try reacquire access to the camera after high priority app take over camera access during recording,that is..whenever camera access is available,it will resume recording.
- use new camera2 api instead of old api.
- absolutely no sound.
- you can configure framerate and bitrate.
- support simultaneous recording using both front and back cameras.(the code support this feathre but whether this's gonna work also depends on whether your device can support this.AFAIK,most devices don't support that because they only have one camera image processor shared by both cameras)
