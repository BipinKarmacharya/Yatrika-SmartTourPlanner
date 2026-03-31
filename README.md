## Yatrika: Smart Trip Planning Application
Yatrika is a comprehensive smart trip planning platform designed to simplify how travelers discover, plan, and share their journeys. From AI-driven itineraries to seamless local payment integrations, Yatrika provides an all-in-one ecosystem for the modern explorer.

### 🚀 Key Features
#### Intelligent Itinerary Management: 
Create and customize trips with specific start/end dates. The system automatically calculates durations and allows users to save, like, or copy existing itineraries.

#### Community Hub: 
A dedicated space for users to create posts, share travel experiences, and review destinations. Includes automated file cleanup on post deletion.

#### Secure Authentication: 
Robust user management featuring OTP-based password resets via email and distinct handling for guest vs. registered users.

#### Local Payment Integration: 
Fully integrated with Khalti for seamless subscription and service payments.

#### Smart Recommendations: 
Tailored destination suggestions based on user interaction and community trends.

### 🛠️ Tech Stack
#### Backend
Framework: Spring Boot 3.x

Database: PostgreSQL

ORM: Hibernate / Spring Data JPA

Mapping: MapStruct (for clean DTO-to-Entity conversions)

Security: Spring Security (OTP & Email-based recovery)

Infrastructure & APIs
Storage: Cloudinary (for media and image hosting)

Payments: Khalti Payment Gateway

External Communications: WebClient (for reactive API consumption)

### 🏗️ Getting Started
#### Prerequisites
JDK 17 or higher

PostgreSQL 15+

Maven 3.6+

Cloudinary Account (for API keys)

Khalti Merchant Account (for payment integration)

### Installation
Clone the repository:

#### Bash
git clone https://github.com/BipinKarmacharya/Yatrika-SmartTourPlanner.git
cd yatrika
#### Configure Environment Variables:
Create an application.yml or application.properties file in src/main/resources with your credentials:

#### YAML
spring:
datasource:
url: jdbc:postgresql://localhost:5432/yatrika_db
username: your_username
password: your_password

cloudinary:
cloud_name: your_cloud_name
api_key: your_api_key
api_secret: your_api_secret

khalti:
secret_key: your_khalti_secret_key
Build and Run:

Bash
mvn clean install
mvn spring-boot:run
### 📂 API Architecture
The project follows a clean, layered architecture:

Controller Layer: Handles REST endpoints.

Service Layer: Contains business logic for trip calculations and payment processing.

Repository Layer: Manages data persistence with PostgreSQL.

DTO Pattern: Ensures secure and efficient data transfer between layers.