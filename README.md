# How to install (GatewayGalaxyProject (Auth))
### Clone [GatewayGalaxyProject](https://github.com/onthelink-nl/gatewaygalaxyproject) using git and go into the directory using a terminal
###### Run `npm install` in the root directory of the project
###### Run `composer install` in the root directory of the project
###### Run `npm run build` in the root directory of the project

### Copy .env.example from GatewayGalaxy to .env and edit necessary details.
###### Make sure to run php artisan key:generate to generate a new key for the application
###### Use a mysql database and run php artisan migrate to create the necessary tables, answer yes to any questions

### Run `php artisan serve` to start the server, or use a tool like Laravel Herd.

---

# How to install (Arduino Uno)
### Get a Arduino Uno and connect it to the computer using a USB cable
### Flash the Arduino with the code in the Arduino directory using the Arduino IDE (ArduinoSketch)

---

# How to install (SUI (ArduinoIntegration branch))
### Clone this repository using git and go into the directory using a terminal
###### Open using a java IDE (IntelliJ IDEA, Eclipse, etc.)
###### Open the dependencies and add the lib directory to the project, apply the changes
###### Run the project using the IDE (MainApplication.java) after connecting the Arduino to the computer
