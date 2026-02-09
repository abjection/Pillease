<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, PUT");

$host = "localhost";
$username = "root";
$password = "";
$database = "pillease_db";

$conn = new mysqli($host, $username, $password, $database);

if ($conn->connect_error) {
    echo json_encode([
        "success" => false,
        "message" => "Database connection failed: " . $conn->connect_error
    ]);
    exit();
}

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $action = $_POST['action'] ?? 'create';
    
    if ($action == 'update') {
        $user_id = $_POST['user_id'] ?? 0;
        $full_name = $_POST['full_name'] ?? '';
        $age = $_POST['age'] ?? 0;
        $email = $_POST['email'] ?? '';
        $gender = $_POST['gender'] ?? null;
        $blood_type = $_POST['blood_type'] ?? null;
        $emergency_contact = $_POST['emergency_contact'] ?? null;

        if (empty($user_id) || empty($full_name) || empty($age) || empty($email)) {
            echo json_encode([
                "success" => false,
                "message" => "Required fields missing"
            ]);
            exit();
        }

        $stmt = $conn->prepare("UPDATE users SET full_name=?, age=?, email=?, gender=?, blood_type=?, emergency_contact=? WHERE user_id=?");
        $stmt->bind_param("sissssi", $full_name, $age, $email, $gender, $blood_type, $emergency_contact, $user_id);

        if ($stmt->execute()) {
            echo json_encode([
                "success" => true,
                "message" => "Profile updated successfully!"
            ]);
        } else {
            echo json_encode([
                "success" => false,
                "message" => "Failed to update profile: " . $stmt->error
            ]);
        }
        $stmt->close();
    } else {
        $full_name = $_POST['full_name'] ?? '';
        $age = $_POST['age'] ?? 0;
        $email = $_POST['email'] ?? '';
        $gender = $_POST['gender'] ?? null;
        $blood_type = $_POST['blood_type'] ?? null;
        $emergency_contact = $_POST['emergency_contact'] ?? null;

        if (empty($full_name) || empty($age) || empty($email)) {
            echo json_encode([
                "success" => false,
                "message" => "Required fields missing"
            ]);
            exit();
        }

        $stmt = $conn->prepare("INSERT INTO users (full_name, age, email, gender, blood_type, emergency_contact) VALUES (?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("sissss", $full_name, $age, $email, $gender, $blood_type, $emergency_contact);

        if ($stmt->execute()) {
            echo json_encode([
                "success" => true,
                "message" => "Profile saved successfully!",
                "user_id" => $stmt->insert_id
            ]);
        } else {
            echo json_encode([
                "success" => false,
                "message" => "Failed to save profile: " . $stmt->error
            ]);
        }
        $stmt->close();
    }
}

else if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $user_id = $_GET['user_id'] ?? null;
    
    if ($user_id) {
        $stmt = $conn->prepare("SELECT * FROM users WHERE user_id = ?");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($row = $result->fetch_assoc()) {
            echo json_encode([
                "success" => true,
                "data" => $row
            ]);
        } else {
            echo json_encode([
                "success" => false,
                "message" => "User not found"
            ]);
        }
        $stmt->close();
    } else {
        $result = $conn->query("SELECT * FROM users ORDER BY created_at DESC");
        
        $users = [];
        while ($row = $result->fetch_assoc()) {
            $users[] = $row;
        }

        echo json_encode([
            "success" => true,
            "data" => $users,
            "count" => count($users)
        ]);
    }
}

$conn->close();
?>
