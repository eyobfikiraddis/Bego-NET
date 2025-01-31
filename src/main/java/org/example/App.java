package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import com.mongodb.client.*;
import org.bson.Document;
import io.javalin.Javalin;
import io.javalin.http.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class App {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static double totalMoney = 0;

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load(); // Loads .env file
        String mongodb = dotenv.get("MONGODB");
        String eyo = dotenv.get("EYOB");
        String s = dotenv.get("SEKINA");
        String m = dotenv.get("MISGANA");
        String ey = dotenv.get("EYUEL");
        String l = dotenv.get("LEUL");
        String b = dotenv.get("BETEL");

        String mongoUrl = "mongodb+srv://begonet:" + mongodb
                + "@cluster0.6bghu.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
        // System.out.println("Database Host: " + mongodb);
        // String dbPort = dotenv.get("DB_PORT");
        mongoClient = MongoClients.create(mongoUrl);
        database = mongoClient.getDatabase("begonetdb");

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.anyHost(); // Allow requests from any origin
                });
            });
        }).start(7000);

        // Donor submission endpoint
        app.post("/submit-donor", ctx -> {
            String name = ctx.formParam("name");
            String email = ctx.formParam("email");
            String phone = ctx.formParam("phone");
            String address = ctx.formParam("address");
            String item = ctx.formParam("donatedItem");
            String quantity = ctx.formParam("quantity");
            String money = ctx.formParam("donationAmount");

            try {
                MongoCollection<Document> collection = database.getCollection("Potential_D");
                Document donor = new Document("name", name)
                        .append("email", email)
                        .append("phone", phone)
                        .append("address", address)
                        .append("item", item == null || item.isEmpty() ? null : item)
                        .append("quantity", quantity == null || quantity.isEmpty() ? null : quantity)
                        .append("money", money == null || money.isEmpty() ? null : money);

                collection.insertOne(donor);
                ctx.status(201).result("Donor information saved successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to save donor information.");
            }
        });

        app.post("/register-receiver", ctx -> {
            // Parse JSON body
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> requestData = objectMapper.readValue(ctx.body(), Map.class);

            // Extract values
            String name = requestData.get("name");
            String phone = requestData.get("phone");
            String address = requestData.get("address");
            String income = requestData.get("income");
            String account = requestData.get("account");

            try {
                System.out.println(name + " " + phone);
                insertReceiver(name, phone, address, income, account);
                ctx.status(201).result("Receiver added successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to add receiver.");
            }
        });

        app.post("/validate-donation", ctx -> {
            String name = ctx.formParam("name");
            name = name.replaceFirst("^\\s*Name:\\s*", "");

            String email = ctx.formParam("email");
            String phone = ctx.formParam("phone");
            String address = ctx.formParam("address");
            String item = ctx.formParam("item");
            String quantity = ctx.formParam("quantity");
            String moneyStr = ctx.formParam("money");

            if (name == null || name.isEmpty()) {
                ctx.status(400).result("Name is required");
                return;
            }

            Double money = null;
            try {
                money = Double.parseDouble(moneyStr);
            } catch (NumberFormatException e) {
                ctx.status(400).result("Invalid money value. It must be a numeric value.");
                return;
            }

            try {
                boolean isDeleted = deletePotential_d(name.trim());
                if (isDeleted) {
                    insertDonor(name, email, phone, address);
                    insertDonation(item, quantity, String.valueOf(money));
                    ctx.status(200).result("Donation validated and processed successfully.");
                } else {
                    ctx.status(404).result("Donation not found in Potential_D.");
                }
            } catch (Exception e) {
                ctx.status(500).result("Error " + e.getMessage());
                e.printStackTrace();
            }
        });

        app.get("/api/donations", ctx -> {
            try {
                ArrayList<Map<String, Object>> donors = getPotentialDonors();
                ctx.json(donors); // Return the list of donors as JSON
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to fetch donations.");
            }
        });

        // app.get("/api/data", ctx -> ctx.json(new ResponseMessage("API is
        // working!")));

        HashMap<String, String> userCredentials = new HashMap<>() {
            {
                put("eyob", eyo);
                put("sekina", s);
                put("misgana", m);
                put("eyuel", ey);
                put("betel", b);
                put("leoul", l);
            }
        };

        // Login endpoint
        app.post("/login", ctx -> handleLogin(ctx, userCredentials));

        app.get("/api/donors", ctx -> {
            try {
                ctx.json(getDonors());
            } catch (Exception e) {
                ctx.status(500).result("Failed to fetch donor data.");
            }
        });

        app.get("/api/items", ctx -> {
            try {
                ctx.json(getDonations());
            } catch (Exception e) {
                ctx.status(500).result("Failed to fetch item data.");
            }
        });

        app.get("/api/distribution", ctx -> {
            try {
                ArrayList<Map<String, Object>> distribution = getDistribution();
                ctx.json(distribution);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to fetch distribution.");
            }
        });

        app.post("/api/distribute", ctx -> {
            try {
                distribute();
                ctx.status(200).result("Distribution completed.");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to distribute items." + e.getMessage());
            }
        });

        app.post("/validate-distribution", ctx -> {
            String name = ctx.formParam("name");
            name = name.replaceFirst("^\\s*Name:\\s*", "");
            String email = ctx.formParam("email");
            String phone = ctx.formParam("phone");
            String address = ctx.formParam("address");
            String item = ctx.formParam("item");
            String quantity = ctx.formParam("quantity");
            String moneyStr = ctx.formParam("money");

            if (name == null || name.isEmpty()) {
                ctx.status(400).result("Name is required");
                return;
            }
            deletedistribution(name.trim());
        });

    }

    public static ArrayList<Map<String, Object>> getPotentialDonors() {
        ArrayList<Map<String, Object>> potentialDonors = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("Potential_D");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Map<String, Object> donor = new HashMap<>();
                donor.put("Name", doc.getString("name"));
                donor.put("Email", doc.getString("email"));
                donor.put("Phone", doc.getString("phone"));
                donor.put("Address", doc.getString("address"));
                donor.put("Item", doc.getString("item"));
                donor.put("Quantity", doc.getString("quantity"));
                donor.put("Money", doc.getString("money"));
                potentialDonors.add(donor);
            }
        }
        return potentialDonors;
    }

    public static boolean deletePotential_d(String name) {
        MongoCollection<Document> collection = database.getCollection("Potential_D");
        long deletedCount = collection.deleteOne(new Document("name", name)).getDeletedCount();
        return deletedCount > 0;
    }

    public static void insertDonor(String name, String email, String phone, String address) {
        MongoCollection<Document> collection = database.getCollection("Donors");
        Document donor = new Document("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("address", address);

        collection.insertOne(donor);
    }

    public static void insertDonation(String item, String quantity, String money) {
        MongoCollection<Document> collectionD = database.getCollection("Donations");

        Document donation = new Document("item", item)
                .append("quantity", quantity)
                .append("money", money);

        collectionD.insertOne(donation);

        MongoCollection<Document> collectionM = database.getCollection("Totalmoney");
        try (MongoCursor<Document> cursor = collectionM.find().iterator()) {
            String Tmoney = "";
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Tmoney = doc.getString("money");
                totalMoney = Double.parseDouble(Tmoney);
            }
            totalMoney += Double.parseDouble(money);
            collectionM.deleteOne(new Document("money", Tmoney)).getDeletedCount();
            Tmoney = String.valueOf(totalMoney);
            Document Mony = new Document("money", Tmoney);
            collectionM.insertOne(Mony);
        }


    }

    private static void handleLogin(Context ctx, HashMap<String, String> userCredentials) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (username == null || password == null) {
            ctx.status(400).result("Username and password are required!");
            return;
        }

        if (userCredentials.getOrDefault(username, "").equals(password)) {
            ctx.redirect("/admin.html");
        } else {
            ctx.status(401).result("Invalid username or password.");
        }
    }

    static class ResponseMessage {
        private String message;

        public ResponseMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static Map<String, Integer> getDonations() {
        MongoCollection<Document> collection = database.getCollection("Donations");
        Map<String, Integer> items = new HashMap<>();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String item = doc.getString("item");

                // Get the 'quantity' as a string and parse it to an integer
                String quantityStr = doc.getString("quantity");

                int quantity = 0; // Default to 0 if parsing fails
                try {
                    if (quantityStr != null && !quantityStr.equals("N/A")) {
                        quantity = Integer.parseInt(quantityStr); // Parse the string to integer
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid quantity value for item: " + item);
                }

                // Add the item and its quantity to the map
                items.put(item, items.getOrDefault(item, 0) + quantity);
            }
        }

        return items;
    }

    public static ArrayList<Map<String, Object>> getDonors() {
        ArrayList<Map<String, Object>> donors = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("Donors");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Map<String, Object> donor = new HashMap<>();
                donor.put("Name", doc.getString("name"));
                donor.put("Email", doc.getString("email"));
                donor.put("Phone", doc.getString("phone"));
                donor.put("Address", doc.getString("address"));
                donors.add(donor);
            }
        }
        return donors;
    }

    public static void insertReceiver(String name, String phone, String location, String account, String incom) {
        MongoCollection<Document> collection = database.getCollection("Receivers");

        int income = 0;
        try {
            income = Integer.parseInt(incom);
        } catch (NumberFormatException e) {
            System.err.println("Invalid income format for receiver: " + name);
        }

        double monetaryAssistance = 0;
        if (income > 1000) {
            monetaryAssistance = 0; // Not Eligible
        } else if (income > 800) {
            monetaryAssistance = 3000;
        } else if (income > 500) {
            monetaryAssistance = 4000;
        } else {
            monetaryAssistance = 5000;
        }
        Document receiver = new Document("name", name)
                .append("contact", phone)
                .append("location", location)
                .append("account", account)
                .append("monetaryAssistance", monetaryAssistance);

        collection.insertOne(receiver);
        System.out.println("Receiver inserted: " + receiver.toJson()); // Debugging log
    }

    public static ArrayList<List<Object>> getReceivers() {
        ArrayList<List<Object>> receiversList = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("Receivers");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                List<Object> receiver = new ArrayList<>();
                receiver.add(doc.getString("name")); // Name
                receiver.add(doc.getString("contact")); // Phone
                receiver.add(doc.getString("location")); // Address
                receiver.add(doc.getString("account")); // Account
                receiver.add(doc.getDouble("monetaryAssistance"));// Monetary Assistance

                receiversList.add(receiver);
            }
        }
        return receiversList;
    }

    public static boolean deleteReceiver(String name) {
        MongoCollection<Document> collection = database.getCollection("Receivers");
        long deletedCount = collection.deleteOne(new Document("name", name)).getDeletedCount();
        return deletedCount > 0;
    }

    public static void insertDistribution(String name, String phone, String address, String money, String account,
            String item, String quantity) {
        MongoCollection<Document> collection = database.getCollection("Distributions");
        Document distribution = new Document("name", name)
                .append("phone", phone)
                .append("address", address)
                .append("money", money)
                .append("account", account)
                .append("item", item)
                .append("quantity", quantity);
        collection.insertOne(distribution);
    }

    public static ArrayList<Map<String, Object>> getDistribution() {
        ArrayList<Map<String, Object>> Distribution = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("Distributions");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Map<String, Object> distribution = new HashMap<>();
                distribution.put("Name", doc.getString("name"));
                distribution.put("Email", doc.getString("email"));
                distribution.put("Phone", doc.getString("phone"));
                distribution.put("Address", doc.getString("address"));
                distribution.put("Item", doc.getString("item"));
                distribution.put("Quantity", doc.getString("quantity"));
                distribution.put("Money", doc.getString("money"));
                Distribution.add(distribution);
            }
        }
        return Distribution;
    }

    public static boolean deletedistribution(String name) {
        MongoCollection<Document> collection = database.getCollection("Distributions");
        long deletedCount = collection.deleteOne(new Document("name", name)).getDeletedCount();
        return deletedCount > 0;
    }

    public static void distribute() {
        ArrayList<List<Object>> receivers = getReceivers();
        Map<String, Integer> donations = getDonations();

        for (List<Object> receiver : receivers) {
            double assistance = 0;
            try {
                assistance = Double.parseDouble(receiver.get(4).toString());
            } catch (NumberFormatException e) {
                System.out.println("Invalid assistance value for receiver: " + receiver.get(4));
                continue;
            }

            String Tmoney = "";
            MongoCollection<Document> collectionM = database.getCollection("Totalmoney");
            try (MongoCursor<Document> cursor = collectionM.find().iterator()) {

                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    Tmoney = doc.getString("money");
                    totalMoney = Double.parseDouble(Tmoney);
                }

            }

            if (totalMoney < assistance) {
                System.out.println("Not enough funds for assistance: " + receiver.get(0));
                String name = receiver.get(0).toString();
                String phone = receiver.get(1).toString();
                String address = receiver.get(2).toString();
                String account = receiver.get(3).toString();

                totalMoney -= assistance;
                collectionM.deleteOne(new Document("money", Tmoney)).getDeletedCount();
                Tmoney = String.valueOf(totalMoney);
                Document Mony = new Document("money", Tmoney);
                collectionM.insertOne(Mony);

                for (Map.Entry<String, Integer> entry : donations.entrySet()) {
                    String item = entry.getKey();
                    int availableQuantity = entry.getValue();
                    int quantity = 0;

                    if (availableQuantity > 0) {
                        quantity = Math.min(2, availableQuantity);
                    }

                    if (quantity > 0) {
                        insertDistribution(name, phone, address, String.format("%.2f", assistance), account, item,
                                String.valueOf(quantity));
                        donations.put(item, availableQuantity - quantity);
                    }
                }

                deleteReceiver(name);
            }


        }
    }
}