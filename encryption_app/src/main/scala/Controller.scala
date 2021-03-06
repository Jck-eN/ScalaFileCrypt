import java.io.File
import java.security.InvalidKeyException

import javafx.fxml.FXML
import javafx.scene.control.{Button, ChoiceBox, ListView, PasswordField, SelectionMode}
import javafx.scene.input.{DragEvent, Dragboard, KeyCode, KeyEvent}
import javafx.stage.DirectoryChooser

import scalafx.stage.{FileChooser, Stage}
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.input.TransferMode

import scala.jdk.javaapi.CollectionConverters
import scala.collection.mutable.ArrayBuffer

class Controller(){

  val fileManager = new FileManager(this)


  @FXML
  var filesList: ListView[File] = _
  @FXML
  var passwordField: PasswordField = _
  @FXML
  var buttonDecrypt: Button = _
  @FXML
  var buttonEncrypt: Button = _
  @FXML
  var buttonCompressAndEncrypt: Button = _
  @FXML
  var choiceAlgorithm: ChoiceBox[String] = _

  @FXML
  def initialize(): Unit = {
    filesList.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)
  }

  @FXML
  def menuFileCloseOnClick(): Unit = {
    System.exit(0)
  }

  @FXML
  def menuHelpAboutOnClick(): Unit = {
    this.showAlert(AlertType.Information,
      "O Aplikacji ",
      "Aplikacja powstała w ramach projektu z przedmiotu:\nProgramowanie w języku Scala",
      "Wersja: 1.0\n\nAutorzy: \n- Kamil Koczera\n- Jacek Nitychoruk")
  }

  @FXML
  def buttonEncryptOnClick(): Unit = {
    var encrypted = 0
    try {
      encrypted = this.fileManager.encryptFiles(passwordField.getText)
    } catch {
      case ex: IllegalStateException =>
        this.showAlert(AlertType.Error,
          "Wystąpił błąd",
          header=ex.getMessage
          )
        return
      case ex: InvalidKeyException =>
        this.showAlert(AlertType.Error,
          "Nieprawidłowe hasło",
          header=ex.getMessage
        )
        return
      case ex: Exception =>
        this.showAlert(AlertType.Error,
          "Nieoczekiwany błąd",
          header=ex.getMessage
        )
        return
    }
    this.showAlert(AlertType.Information,
      "Szyfrowanie zakończone",
      "Zakończono pomyślnie szyfrowanie plików",
      "Zaszyfrowano pomyślnie: " + encrypted + "\\" + this.fileManager.numberOfFiles)
    this.passwordField.setText("")
    this.fileManager.onFilesChange()
  }

  @FXML
  def buttonDecryptOnClick(): Unit = {
    var decrypted = 0
    val allFiles = this.fileManager.numberOfFiles()
    try {
      if(allFiles == 0) {
        throw new IllegalStateException("Nie wybrano plików")
      }
      if(passwordField.getText.length < 3) {
        throw new InvalidKeyException("Hasło musi mieć przynajmniej 3 znaki")
      }
      val stage = new Stage()
      val directoryChooser = new DirectoryChooser()
      directoryChooser.setTitle("Wybierz miejsce do zapisu odszyfrowanych plików")
      val selectedDirectory = directoryChooser.showDialog(stage)
      if (selectedDirectory != null) {
        decrypted = this.fileManager.decryptFiles(passwordField.getText, selectedDirectory)
        if (decrypted == allFiles){
          this.showAlert(AlertType.Information,
            "Deszyfrowanie zakończone",
            "Zakończono pomyślnie odszyfrowywanie plików",
            "Odszyfrowano wszystkie " + decrypted + " plików.")
        }
        else{
          this.showAlert(AlertType.Information,
            "Deszyfrowanie zakończone",
            "Wystąpiły błędy podczas odszyfrowywania plików",
            "Odszyfrowano " + decrypted + "\\" + allFiles)
        }

        this.passwordField.setText("")
        this.fileManager.onFilesChange()
      }
    } catch{
      case ex: IllegalStateException =>
        this.showAlert(AlertType.Error,
          "Wystąpił błąd",
          header=ex.getMessage
        )
      case ex: InvalidKeyException =>
        this.showAlert(AlertType.Error,
          "Hasło nie spełnia wymagań",
          header=ex.getMessage
        )
      case ex: Exception =>
        this.showAlert(AlertType.Error,
          "Nieoczekiwany błąd",
          header = ex.getMessage
        )
    }

  }

  @FXML
  def buttonCompressFilesOnClick(): Unit = {
    var compressed = 0
    val allFiles = this.fileManager.numberOfFiles()
    try {
      if(passwordField.getText.length < 3) {
        throw new InvalidKeyException("Hasło musi mieć przynajmniej 3 znaki")
      }
      if(allFiles == 0) {
        throw new IllegalStateException("Nie wybrano plików")
      }
      val stage = new Stage()
      val fileChooser = new FileChooser()

      val extFilter = new FileChooser.ExtensionFilter("ENC files (*.enc)", "*.enc")
      fileChooser.getExtensionFilters.add(extFilter)

      val file = fileChooser.showSaveDialog(stage)
      if(file != null) {
        compressed = this.fileManager.compressFiles(file, passwordField.getText())

        this.showAlert(AlertType.Information,
          "Kompresja zakończona",
          "Zakończono pomyślnie kompresowanie plików",
          "Skompresowano pomyślnie: " + compressed + "\\" + allFiles)
      }
    } catch {
      case ex: IllegalStateException =>
        this.showAlert(AlertType.Error,
          "Wystąpił błąd",
          header=ex.getMessage
        )
        return
      case ex: Exception =>
        this.showAlert(AlertType.Error,
          "Nieoczekiwany błąd",
          header=ex.getMessage
        )
        return
    }
    this.passwordField.setText("")
    this.fileManager.onFilesChange()
  }

  @FXML
  def buttonSelectFilesOnClick(): Unit = {
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Wybierz pliki")
    val stage = new Stage()
    val files = fileChooser.showOpenMultipleDialog(stage)
    if (files != null){
      this.addFiles(files)
    }
  }

  def deleteSelectedFiles(): Unit = {
    val selectedFiles : ArrayBuffer[File] = ArrayBuffer.empty
    val it = this.filesList.getSelectionModel.getSelectedItems.iterator()
    it.forEachRemaining(selectedFiles.addOne)
    this.removeFiles(selectedFiles.toSeq)
  }

  @FXML
  def buttonDeleteSelectedFilesOnClick(): Unit = {
    deleteSelectedFiles()
  }

  @FXML
  def buttonDeleteSelectedFilesOnKeyPress(event: KeyEvent): Unit = {
    if(event.getCode == KeyCode.DELETE) {
      deleteSelectedFiles()
    }
  }

  @FXML
  def buttonCleanFilesOnClick(): Unit = {
    this.fileManager.clearFiles()
  }

  @FXML
  def choiceAlgorithmOnChange(): Unit = {
    val algorithm = this.getAlgorithmString(this.choiceAlgorithm.getValue)
    this.fileManager.EncryptionAlgorithm = algorithm
  }

  def getAlgorithmString(input: String): String = input match {
    case "AES" => "AES"
    case "Blowfish" => "Blowfish"
    case _ => "AES"
  }

  def updateFileListView(): Unit = {
    this.filesList.getItems.clear()
    if (this.fileManager.files.nonEmpty) {
      for (file <- this.fileManager.files) {
        this.filesList.getItems.add(file)
      }
    }
  }

  def draggedFileOver(event: DragEvent): Unit = {
    if (event.getDragboard.hasFiles) {
      event.acceptTransferModes(TransferMode.Copy)
    }
    event.consume()
  }

  def droppedFile(event: DragEvent): Unit = {
    val db : Dragboard = event.getDragboard
    if(db.hasFiles){
      val droppedFiles = db.getFiles
      this.addFiles(CollectionConverters.asScala(droppedFiles).toSeq)
    }
    event.setDropCompleted(true)
  }

  def showAlert(alertType: AlertType, title : String, header : String = null, content: String = null): Option[ButtonType] = {
    val alert = new Alert(alertType)
    alert.setTitle(title)
    alert.setHeaderText(header)
    alert.setContentText(content)
    alert.showAndWait()
  }

  def addFiles(files: Seq[File]): Unit = {
    this.fileManager.addFiles(files)
  }

  def removeFiles(files: Seq[File]): Unit = {
    this.fileManager.removeFiles(files)
  }

  def setButtonDecryptStatus(status: Boolean): Unit = {
    this.buttonDecrypt.setDisable(!status)
  }

  def setButtonStatus(): Unit = {
    this.buttonDecrypt.setDisable(! fileManager.checkIfAllEncrypted())
    val res = this.fileManager.files.isEmpty
    this.buttonEncrypt.setDisable(res)
    this.buttonCompressAndEncrypt.setDisable(this.fileManager.files.isEmpty)
  }
}
