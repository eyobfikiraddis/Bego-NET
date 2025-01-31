
# በጎ Net

## Overview
This project is a Java-based charity management system that facilitates the interaction between donors and recipients. It uses MongoDB as the database and Javalin as the web framework. The system allows donors to submit their information, validates donations, and processes recipient requests for assistance.

## Features
1. **Donor Management**:
   - Submit donor details such as name, email, phone, address, donated item, quantity, and monetary donations.
   - Retrieve potential donor information.
2. **Recipient Management**:
   - Submit recipient details, including name, age, phone, address, occupation, income, account number, family size, and role in the family.
   - Validate recipient income to determine eligibility for monetary assistance.
3. **Donation Validation**:
   - Validate donations from potential donors and add them to the donor database upon approval.
4. **User Authentication**:
   - Secure admin login functionality.
   
## Technology Stack
- **Programming Language**: Java, Javascript
- **Framework**: Javalin (Web Framework)
- **Database**: MongoDB
- **Build Tool**: Maven

## Prerequisites
- Install [Java](https://www.oracle.com/java/technologies/javase-downloads.html).
- Install [Maven](https://maven.apache.org/install.html).
- Install [MongoDB](https://www.mongodb.com/try/download/community).
- Configure a MongoDB Atlas cluster and replace the connection string in the `mongoUrl` variable.

## Installation
1. Clone this repository:
   ```bash
   git clone <repository-url>
   cd Begonet
   ```

2. Install dependencies using Maven:
   ```bash
   mvn clean install
   ```

3. Set up the database in MongoDB:
   - Create a database named `begonetdb`.
   - Add collections: `Potential_D`, `Receivers`, `Donors`, and `Donations`.

4. Update the `mongoUrl` variable in the `App` class with your MongoDB connection string.


## Directory Structure
```
src
├── main
│   ├── java
│   │   └── org.example
│   │       └── App.java
│   └── resources
│       └── public
│           └── (HTML, CSS, and JS files for the frontend)
```

## በጎ-Net is
A charity management system developed by 3rd year students of Electrical and computer Engineering at Addis Ababa University

## Group Members

| Group Members       | ID            |
|---------------------|---------------|
| BETEL YOHANNES      | UGR/2915/15   |
| EYOB FIKIRADDIS     | UGR/8200/15   |
| EYUEL ENGIDA        | UGR/6642/15   |
| LEOUL TEFERI        | UGR/8372/15   |
| MISGANA MESSAY      | UGR/1524/15   |
| SEKINA NESRIE       | UGR/4634/15   |

---
Developed by the በጎNet team.

