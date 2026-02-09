<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, DELETE");

$host = "localhost";
$username = "root";
$password = "";
$database = "pillease_db";

$conn = new mysqli($host, $username, $password, $database);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Database connection failed"]);
    exit();
}

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $action = $_POST['action'] ?? 'create';
    
    if ($action == 'delete') {
        $log_id = $_POST['log_id'] ?? 0;
        $stmt = $conn->prepare("DELETE FROM daily_health_log WHERE log_id = ?");
        $stmt->bind_param("i", $log_id);
        
        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Log deleted"]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to delete"]);
        }
        $stmt->close();
    } else {
        $user_id = $_POST['user_id'] ?? null;
        $mood = $_POST['mood'] ?? '';
        $temperature = $_POST['temperature'] ?? null;
        $heart_rate = $_POST['heart_rate'] ?? null;
        $symptoms = $_POST['symptoms'] ?? '';
        $log_date = $_POST['log_date'] ?? date('Y-m-d');
        $log_time = $_POST['log_time'] ?? date('H:i:s');

        $stmt = $conn->prepare("INSERT INTO daily_health_log (user_id, mood, temperature, heart_rate, symptoms, log_date, log_time) VALUES (?, ?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("isdisss", $user_id, $mood, $temperature, $heart_rate, $symptoms, $log_date, $log_time);

        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Health log saved!", "log_id" => $stmt->insert_id]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to save log"]);
        }
        $stmt->close();
    }
}

else if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $user_id = $_GET['user_id'] ?? null;
    
    if ($user_id) {
        $stmt = $conn->prepare("SELECT * FROM daily_health_log WHERE user_id = ? ORDER BY log_date DESC, log_time DESC");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        
        $logs = [];
        while ($row = $result->fetch_assoc()) {
            $logs[] = $row;
        }
        echo json_encode(["success" => true, "data" => $logs]);
        $stmt->close();
    } else {
        echo json_encode(["success" => false, "message" => "User ID required"]);
    }
}

$conn->close();
?>
