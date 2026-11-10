import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    private static final Scanner IN = new Scanner(System.in);
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        new TradingPlatform().run();
    }
    static class Stock {
        private final String symbol;
        private final String companyName;
        private double price;
        private double changePercent;

        Stock(String symbol, String companyName, double price) {
            this(symbol, companyName, price, 0);
        }
        Stock(String symbol, String companyName, double price, double changePercent) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.price = price;
            this.changePercent = changePercent;
        }

        String getSymbol() { return symbol; }
        String getCompanyName() { return companyName; }
        double getPrice() { return price; }
        double getChangePercent() { return changePercent; }

        void updatePrice(double newPrice) {
            if (newPrice <= 0)
                throw new IllegalArgumentException("Price must be positive.");

            changePercent = ((newPrice - price) / price) * 100;
            price = newPrice;
        }
    }

    static class Transaction {
        private final String timestamp;
        private final String symbol;
        private final String action;
        private final int quantity;
        private final double price;

        Transaction(String symbol, String action, int quantity, double price) {
            this(LocalDateTime.now().format(FORMAT), symbol, action, quantity, price);
        }

        Transaction(String timestamp, String symbol, String action,
                    int quantity, double price) {
            this.timestamp = timestamp;
            this.symbol = symbol;
            this.action = action;
            this.quantity = quantity;
            this.price = price;
        }

        String toFileLine() {
            return timestamp + "|" + symbol + "|" + action + "|"
                    + quantity + "|" + price;
        }

        void display() {
            
            System.out.printf("%-20s %-7s %-12s %6d %,12.2f %,14.2f%n",
                    timestamp, action, symbol, quantity, price,
                    quantity * price);
        }
    }

    
    static class PortfolioSnapshot {
        private final String timestamp;
        private final double cash;
        private final double holdingsValue;
        private final double totalValue;

        PortfolioSnapshot(double cash, double holdingsValue) {
            this(LocalDateTime.now().format(FORMAT), cash, holdingsValue);
        }

        PortfolioSnapshot(String timestamp, double cash, double holdingsValue) {
            this.timestamp = timestamp;
            this.cash = cash;
            this.holdingsValue = holdingsValue;
            this.totalValue = cash + holdingsValue;
        }

        double getTotalValue() { return totalValue; }

        void display() {
            System.out.printf(
                    "%-20s Cash: ₹%,.2f | Shares: ₹%,.2f | Total: ₹%,.2f%n",
                    timestamp, cash, holdingsValue, totalValue);
        }

        String toFileLine() {
            return timestamp + "|" + cash + "|" + holdingsValue;
        }
    }

    
    static class User {
        private final String username;
        private double cashBalance;

        private final Map<String, Integer> holdings = new LinkedHashMap<>();
        private final List<Transaction> transactions = new ArrayList<>();
        private final List<PortfolioSnapshot> history = new ArrayList<>();

        User(String username) {
            this(username, 100000);
        }

        User(String username, double cashBalance) {
            this.username = username;
            this.cashBalance = cashBalance;
        }

        String getUsername() { return username; }
        double getCashBalance() { return cashBalance; }

        Map<String, Integer> getHoldings() { return holdings; }
        List<Transaction> getTransactions() { return transactions; }
        List<PortfolioSnapshot> getHistory() { return history; }

        void buy(Stock stock, int quantity) {
            validateQuantity(quantity);

            double total = stock.getPrice() * quantity;

            if (total > cashBalance)
                throw new IllegalArgumentException("Insufficient balance.");

            cashBalance -= total;

            holdings.put(stock.getSymbol(),
                    holdings.getOrDefault(stock.getSymbol(), 0) + quantity);

            transactions.add(new Transaction(
                    stock.getSymbol(), "BUY", quantity, stock.getPrice()));

            System.out.printf("Purchase successful! Total: ₹%,.2f%n", total);
        }

        void sell(Stock stock, int quantity) {
            validateQuantity(quantity);

            int owned = holdings.getOrDefault(stock.getSymbol(), 0);

            if (quantity > owned)
                throw new IllegalArgumentException("You do not own enough shares.");

            double total = stock.getPrice() * quantity;
            cashBalance += total;

            if (quantity == owned)
                holdings.remove(stock.getSymbol());
            else
                holdings.put(stock.getSymbol(), owned - quantity);

            transactions.add(new Transaction(
                    stock.getSymbol(), "SELL", quantity, stock.getPrice()));

            System.out.printf("Sale successful! Total: ₹%,.2f%n", total);
        }

        private void validateQuantity(int quantity) {
            if (quantity <= 0)
                throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        double holdingsValue(Map<String, Stock> market) {
            double value = 0;
            for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                Stock stock = market.get(entry.getKey());
                if (stock != null)
                    value += stock.getPrice() * entry.getValue();
            }
            return value;
        }

        double totalValue(Map<String, Stock> market) {
            return cashBalance + holdingsValue(market);
        }

        void recordSnapshot(Map<String, Stock> market) {
            history.add(new PortfolioSnapshot(cashBalance, holdingsValue(market)));
        }
    }


    static class TradingPlatform {
        private final Map<String, Stock> market = new LinkedHashMap<>();
        private final Map<String, User> users = new LinkedHashMap<>();
        private final Path dataFile = Path.of("data", "platform_data.txt");

        TradingPlatform() {
            seedMarket();
            loadData();
        }

        private void seedMarket() {
            market.put("RELIANCE", new Stock("RELIANCE", "Reliance Industries", 1450));
            market.put("TCS", new Stock("TCS", "Tata Consultancy Services", 3820));
            market.put("INFY", new Stock("INFY", "Infosys", 1680));
            market.put("HDFCBANK", new Stock("HDFCBANK", "HDFC Bank", 1740));
            market.put("ITC", new Stock("ITC", "ITC Limited", 430));
            market.put("TATAMOTORS", new Stock("TATAMOTORS", "Tata Motors", 720));
        }

        private User getOrCreateUser(String username) {
            String key = username.trim().toLowerCase(Locale.ROOT);

            if (key.isEmpty())
                throw new IllegalArgumentException("Username cannot be empty.");

            if (!users.containsKey(key)) {
                User created = new User(username.trim());
                users.put(key, created);
                created.recordSnapshot(market); 
                System.out.println("New account created with ₹1,00,000 virtual cash.");
                saveData();
            }

            return users.get(key);
        }

        void displayMarket() {
            System.out.println("\n========== STOCK MARKET ==========");

            System.out.printf("%-13s %-31s %15s %13s%n",
                    "SYMBOL", "COMPANY", "PRICE (₹)", "CHANGE");

            for (Stock stock : market.values()) {
                System.out.printf("%-13s %-31s %,15.2f %+12.2f%%%n",
                        stock.getSymbol(), stock.getCompanyName(),
                        stock.getPrice(), stock.getChangePercent());
            }

            System.out.println("==================================");
            System.out.println("Prices are simulated, not live market data.");
        }

        void displayPortfolio(User user) {
            System.out.println("\n========== MY PORTFOLIO ==========");
            System.out.println("Username: " + user.getUsername());
            System.out.printf("Available Cash: ₹%,.2f%n", user.getCashBalance());

            System.out.printf("%-15s %10s %15s %18s%n",
                    "SYMBOL", "QUANTITY", "PRICE", "VALUE");

            for (Map.Entry<String, Integer> entry : user.getHoldings().entrySet()) {
                Stock stock = market.get(entry.getKey());
                if (stock != null) {
                    System.out.printf("%-15s %10d %,15.2f %,18.2f%n",
                            entry.getKey(), entry.getValue(),
                            stock.getPrice(), entry.getValue() * stock.getPrice());
                }
            }

            System.out.printf("Holdings Value: ₹%,.2f%n", user.holdingsValue(market));
            System.out.printf("Total Portfolio Value: ₹%,.2f%n", user.totalValue(market));
        }

        void displayTransactions(User user) {
            System.out.println("\n========== TRANSACTION HISTORY ==========");

            if (user.getTransactions().isEmpty()) {
                System.out.println("No transactions recorded.");
                return;
            }

            System.out.printf("%-20s %-7s %-12s %6s %12s %14s%n",
                    "DATE", "ACTION", "SYMBOL", "QTY", "PRICE", "TOTAL");

            for (Transaction transaction : user.getTransactions())
                transaction.display();
        }

        void displayPerformance(User user) {
            user.recordSnapshot(market);

            List<PortfolioSnapshot> history = user.getHistory();

            System.out.println("\n========== PORTFOLIO PERFORMANCE ==========");

            if (history.size() < 2) {
                System.out.println("Baseline saved. Check again after trading.");
            } else {
                double first = history.get(0).getTotalValue();
                double latest = history.get(history.size() - 1).getTotalValue();
                double difference = latest - first;
                double percentage = first == 0 ? 0 : difference / first * 100;

                System.out.printf("Initial Value: ₹%,.2f%n", first);
                System.out.printf("Current Value: ₹%,.2f%n", latest);
                System.out.printf("Profit/Loss: ₹%+,.2f (%+.2f%%)%n", difference, percentage);
            }

            System.out.println("\nRecent Performance:");

            for (int i = Math.max(0, history.size() - 5); i < history.size(); i++)
                history.get(i).display();

            saveData();
        }

        void simulatePriceChange() {
            displayMarket();

            System.out.print("Enter stock symbol: ");
            Stock stock = market.get(readLine().trim().toUpperCase(Locale.ROOT));

            if (stock == null) {
                System.out.println("Stock not found.");
                return;
            }

            double newPrice = readDouble("Enter new price: ");

            try {
                stock.updatePrice(newPrice);
                System.out.printf("New price: ₹%,.2f%n", stock.getPrice());
                System.out.printf("Price change: %+.2f%%%n", stock.getChangePercent());
                saveData();
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }

        void run() {
            System.out.println("========================================");
            System.out.println("        STOCK TRADING PLATFORM");
            System.out.println("========================================");

            System.out.print("Enter username: ");

            User user;

            try {
                user = getOrCreateUser(readLine());
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
                return;
            }

            boolean running = true;

            while (running) {
                System.out.println("\nWelcome, " + user.getUsername());

                System.out.println("\n1. View Market");
                System.out.println("2. Buy Stocks");
                System.out.println("3. Sell Stocks");
                System.out.println("4. View Portfolio");
                System.out.println("5. Transaction History");
                System.out.println("6. Portfolio Performance");
                System.out.println("7. Update Stock Prices");
                System.out.println("8. Save and Exit");

                System.out.print("\nEnter your choice: ");

                switch (readLine().trim()) {
                    case "1": displayMarket(); break;
                    case "2": trade(user, true); break;
                    case "3": trade(user, false); break;
                    case "4": displayPortfolio(user); break;
                    case "5": displayTransactions(user); break;
                    case "6": displayPerformance(user); break;
                    case "7": simulatePriceChange(); break;
                    case "8":
                        saveData();
                        System.out.println("Data saved. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }
        }

        private void trade(User user, boolean buying) {
            displayMarket();

            System.out.print("Enter stock symbol: ");
            Stock stock = market.get(readLine().trim().toUpperCase(Locale.ROOT));

            if (stock == null) {
                System.out.println("Invalid stock symbol.");
                return;
            }

            int quantity = readInt("Enter quantity: ");

            try {
                if (buying)
                    user.buy(stock, quantity);
                else
                    user.sell(stock, quantity);

                user.recordSnapshot(market);
                saveData();
            } catch (IllegalArgumentException ex) {
                System.out.println("Transaction failed: " + ex.getMessage());
            }
        }

        private String readLine() {
            if (!IN.hasNextLine()) {
                saveData();
                System.out.println("\nInput closed. Data saved. Goodbye!");
                System.exit(0);
            }
            return IN.nextLine();
        }

        private int readInt(String message) {
            while (true) {
                System.out.print(message);
                try {
                    return Integer.parseInt(readLine().trim());
                } catch (NumberFormatException ex) {
                    System.out.println("Please enter a whole number.");
                }
            }
        }

        private double readDouble(String message) {
            while (true) {
                System.out.print(message);
                try {
                    return Double.parseDouble(readLine().trim());
                } catch (NumberFormatException ex) {
                    System.out.println("Please enter a valid number.");
                }
            }
        }

        private void saveData() {
            try {
                Files.createDirectories(dataFile.getParent());

                List<String> lines = new ArrayList<>();

                for (Stock stock : market.values()) {
                    lines.add("STOCK\t" + stock.getSymbol() + "\t"
                            + stock.getCompanyName() + "\t"
                            + stock.getPrice() + "\t"
                            + stock.getChangePercent());
                }

                for (User user : users.values()) {
                    String name = user.getUsername().replace("\t", " ");

                    lines.add("USER\t" + name + "\t" + user.getCashBalance());

                    for (Map.Entry<String, Integer> entry : user.getHoldings().entrySet())
                        lines.add("HOLDING\t" + name + "\t" + entry.getKey() + "\t" + entry.getValue());

                    for (Transaction transaction : user.getTransactions())
                        lines.add("TRANSACTION\t" + name + "\t" + transaction.toFileLine());

                    for (PortfolioSnapshot snapshot : user.getHistory())
                        lines.add("SNAPSHOT\t" + name + "\t" + snapshot.toFileLine());
                }

                Files.write(dataFile, lines, StandardCharsets.UTF_8);

            } catch (IOException ex) {
                System.out.println("Save error: " + ex.getMessage());
            }
        }

        private void loadData() {
            if (!Files.exists(dataFile))
                return;

            try {
                for (String line : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\t", -1);

                    switch (p[0]) {
                        case "STOCK":
                            if (p.length >= 5 && market.containsKey(p[1])) {
                                market.put(p[1], new Stock(p[1], p[2],
                                        Double.parseDouble(p[3]),
                                        Double.parseDouble(p[4])));
                            }
                            break;

                        case "USER":
                            if (p.length >= 3) {
                                users.put(p[1].toLowerCase(Locale.ROOT),
                                        new User(p[1], Double.parseDouble(p[2])));
                            }
                            break;

                        case "HOLDING":
                            if (p.length >= 4) {
                                User u = users.get(p[1].toLowerCase(Locale.ROOT));
                                if (u != null)
                                    u.getHoldings().put(p[2], Integer.parseInt(p[3]));
                            }
                            break;

                        case "TRANSACTION":
                            if (p.length >= 3) {
                                User u = users.get(p[1].toLowerCase(Locale.ROOT));
                                if (u != null) {
                                    String[] t = p[2].split("\\|", -1);
                                    if (t.length == 5) {
                                        
                                        u.getTransactions().add(new Transaction(
                                                t[0], t[1], t[2],
                                                Integer.parseInt(t[3]),
                                                Double.parseDouble(t[4])));
                                    }
                                }
                            }
                            break;

                        case "SNAPSHOT":
                            if (p.length >= 3) {
                                User u = users.get(p[1].toLowerCase(Locale.ROOT));
                                if (u != null) {
                                    String[] s = p[2].split("\\|", -1);
                                    if (s.length == 3) {
                                        u.getHistory().add(new PortfolioSnapshot(
                                                s[0],
                                                Double.parseDouble(s[1]),
                                                Double.parseDouble(s[2])));
                                    }
                                }
                            }
                            break;

                        default:
                            break;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Warning: Could not load saved data: " + ex.getMessage());
            }
        }
    }
}
