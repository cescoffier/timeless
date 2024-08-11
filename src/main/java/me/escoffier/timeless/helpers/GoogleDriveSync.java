package me.escoffier.timeless.helpers;

import com.google.api.services.drive.model.FileList;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.escoffier.timeless.inboxes.google.Account;
import me.escoffier.timeless.inboxes.google.GoogleAccounts;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class GoogleDriveSync {


    @Inject
    GoogleAccounts accounts;

    @ConfigProperty(name = "google-drive.sync.local-directory")
    Optional<File> local;

    @ConfigProperty(name = "google-drive.sync.remote-directory")
    Optional<String> remote;

    public void sync() throws IOException {
        if (local.isEmpty()  || remote.isEmpty()) {
            return;
        }

        File directory = local.get();
        String remoteFolder = remote.get();
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }

        for (Map.Entry<String, Account> entry : accounts.accounts().entrySet()) {
            FileList list = entry.getValue().drive().files().list()
                    .setQ("'root' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, parents)")
                    .execute();
            for (com.google.api.services.drive.model.File file : list.getFiles()) {
                if (remoteFolder.equals(file.getName())) {
                    Log.infof("Remote folder %s found in '%s' Google Drive. Id is %s", remoteFolder, entry.getKey(), file.getId());
                    sync(entry.getValue(), directory, file);
                }
            }

        }

    }

    private void sync(Account account, File dir, com.google.api.services.drive.model.File remote) throws IOException {
        FileList list = account.drive().files().list()
                .setQ("'" + remote.getId() + "' in parents and trashed = false")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, parents)")
                .execute();
        for (com.google.api.services.drive.model.File file : list.getFiles()) {
            if (file.getName().endsWith(".pdf")) {
                download(account, dir, file);
            }
        }


    }

    private static void download(Account account, File dir, com.google.api.services.drive.model.File file) throws IOException {
        File out = new File(dir, file.getName());
        if (out.isFile()) {
            Log.debugf("Skipping the download of file %s (%s), %s already exists", file.getName(), file.getId(), out.getAbsolutePath());
            return;
        }
        Log.infof("Downloading file %s (%s) to %s", file.getName(), file.getId(), out.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(out)){
            account.drive().files().get(file.getId())
                    .executeMediaAndDownloadTo(fos);
        }
    }

}
