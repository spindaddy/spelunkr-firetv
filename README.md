# SPELUNKR Fire TV

Version 8 opens the SPELUNKR channel guide, plays the selected channel full screen, and stops the video when you press Home.

After the app is installed, the Fire Stick loads [spelunkr.tv](https://spelunkr.tv/) over the internet. The computer you used for the download can be off.

## Download with Downloader

1. On the Fire Stick, open the Amazon Appstore and search for **Downloader**. Install the orange Downloader app by AFTVnews.

2. Allow apps from unknown sources.
   - Go to **Settings → My Fire TV → About**.
   - Highlight the device name and press Select seven times, until Developer options are unlocked.
   - Go back to **My Fire TV → Developer options → Apps from Unknown Sources** and turn it **On**.
   - On some Fire OS versions the switch is **Settings → Applications → Appstore → App from Unknown Sources**.

3. Open Downloader. If it asks for permission to install apps, allow it.

4. In the URL box, enter this address and choose **Go**:

   `https://cdn.jsdelivr.net/gh/spindaddy/spelunkr-firetv@v8/release/SPELUNKR-FireTV.apk`

5. When the download finishes, choose **Install**, then **Open** or **Done**.

6. Find **SPELUNKR** under **Your Apps & Channels**. If it is not on the first row, choose **See all apps**.

The same file is attached to the [Version 8 release](https://github.com/spindaddy/spelunkr-firetv/releases/tag/v8).

## Remote

- **Down** and **Up** move through the guide.
- **Select** or **Play** watches the highlighted channel full screen.
- **Back** returns to the guide.
- **Home** leaves the app and stops playback.

## If the install fails

- A 404 means the address was typed wrong. Use the address above exactly.
- If Fire OS blocks the install, open Downloader, allow it to install unknown apps, and download the file again.
- If an older SPELUNKR sideload is already installed, install this file over it. If Fire OS refuses the update, uninstall that SPELUNKR app and install again.
- The 2025 Fire TV Stick 4K Select and the 2026 Fire TV Stick HD use Vega OS, which cannot install APK files.
