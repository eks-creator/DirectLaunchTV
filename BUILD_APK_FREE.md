# Build the APK for free with GitHub Actions

1. Create a free GitHub account if you do not already have one.
2. Create a new repository. Public is simplest because standard GitHub Actions runners are free for public repositories.
3. Upload **all files and folders from this project root**, including the hidden `.github` folder.
4. Open the repository's **Actions** tab.
5. Select **Build DirectLaunch TV APK**.
6. Choose **Run workflow**.
7. When the workflow finishes, open the completed run.
8. Under **Artifacts**, download **DirectLaunchTV-debug-apk**.
9. Unzip that artifact. The installable file is `app-debug.apk`.

The debug APK is signed automatically with the standard Android debug key and is suitable for sideloading/testing on an Android TV or Google TV device.
