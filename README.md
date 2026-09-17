# ValexAchievements

Custom achievement and title foundation for ValexSMP.

## Build on GitHub

1. Create a new GitHub repository.
2. Upload the contents of this project to the repository root.
3. Open **Actions**.
4. Select **Build ValexAchievements**.
5. Click **Run workflow**.
6. When the workflow finishes, open the workflow run and download the artifact.

The workflow uses Java 25 and Maven, then produces:

`ValexAchievements.jar`

## Local plugin data

The plugin stores player data in SQLite under:

`plugins/ValexAchievements/data.db`

Do not upload the server's generated `data.db` to GitHub.
