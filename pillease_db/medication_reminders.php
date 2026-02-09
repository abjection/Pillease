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
        $reminder_id = $_POST['reminder_id'] ?? 0;
        $stmt = $conn->prepare("DELETE FROM medication_reminders WHERE reminder_id = ?");
        $stmt->bind_param("i", $reminder_id);
        
        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Reminder deleted"]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to delete"]);
        }
        $stmt->close();
    } else {
        $user_id = $_POST['user_id'] ?? null;
        $medication_name = $_POST['medication_name'] ?? '';
        $reminder_time = $_POST['reminder_time'] ?? '';
        $frequency = $_POST['frequency'] ?? '';

        if (empty($medication_name) || empty($reminder_time)) {
            echo json_encode(["success" => false, "message" => "Required fields missing"]);
            exit();
        }

        $stmt = $conn->prepare("INSERT INTO medication_reminders (user_id, medication_name, reminder_time, frequency) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("isss", $user_id, $medication_name, $reminder_time, $frequency);

        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Reminder saved!", "reminder_id" => $stmt->insert_id]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to save reminder"]);
        }
        $stmt->close();
    }
}

else if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $user_id = $_GET['user_id'] ?? null;
    
    if ($user_id) {
        $stmt = $conn->prepare("SELECT * FROM medication_reminders WHERE user_id = ? ORDER BY reminder_time ASC");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        
        $reminders = [];
        while ($row = $result->fetch_assoc()) {
            $reminders[] = $row;
        }
        echo json_encode(["success" => true, "data" => $reminders]);
        $stmt->close();
    } else {
        echo json_encode(["success" => false, "message" => "User ID required"]);
    }
}

$conn->close();
?>
