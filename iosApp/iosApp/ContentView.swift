import SwiftUI
import Shared
import UserNotifications

class QueueManagerViewModel: ObservableObject, RequestQueueManagerListener {
    @Published var highPriorityQueueLength: Int32 = 0
    @Published var mediumPriorityQueueLength: Int32 = 0
    @Published var errorMessage: String? = nil
    
    deinit {
        HttpRequestManager.shared.removeListener(listener: self)
    }
    
    init() {
        HttpRequestManager.shared.addListener(listener: self)
    }
    
    func onError(message: String) {
        DispatchQueue.main.async {
            self.errorMessage = message
            self.showNotification(title: "Error", message: message)
        }
    }
    
    func onQueueCountChanged() {
        let highCount = HttpRequestManager.shared.getHighPriorityQueueLength()
        let mediumCount = HttpRequestManager.shared.getMediumPriorityQueueLength()
        
        Task {
            await MainActor.run {
                self.highPriorityQueueLength = highCount
                self.mediumPriorityQueueLength = mediumCount
            }
        }
    }
    
    func showNotification(title: String, message: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}

struct ContentView: View {
    @StateObject private var viewModel = QueueManagerViewModel()
    
    init() {
        //        startBackgroundProcessing()
    }
    
    func sendRequest(url: String, queueType: QueueType) {
        let request = HttpRequest(url: url, method: .get, headers: [:], body: nil)
        Task {
            try await HttpRequestManager.shared.sendRequest(request: request, queueType: queueType)
        }
    }
    
    var body: some View {
        VStack(spacing: 16) {
            Button(action: {
                sendRequest(url: "https://jsonplaceholder.typicode.com/todos/", queueType: .highPriority)
            }) {
                Text("Send High Priority Request")
            }
            .frame(maxWidth: .infinity)
            
            Button(action: {
                sendRequest(url: "https://jsonplaceholder.typicode.com/todos/", queueType: .mediumPriority)
            }) {
                Text("Send Medium Priority Request")
            }
            .frame(maxWidth: .infinity)
            
            Button(action: {
                sendRequest(url: "https://jsonplaceholder.typicode.com1/todos/", queueType: .highPriority)
            }) {
                Text("Send High Priority Request with DNS error")
            }
            .frame(maxWidth: .infinity)
            
            Button(action: {
                sendRequest(url: "https://jsonplaceholder.typicode.com/error", queueType: .highPriority)
            }) {
                Text("Send High Priority Request with error")
            }
            .frame(maxWidth: .infinity)
            
            Text("High Priority Queue Length: \(viewModel.highPriorityQueueLength)")
            Text("Medium Priority Queue Length: \(viewModel.mediumPriorityQueueLength)")
        }
        .navigationTitle("Queue Manager")
        .padding()
        .alert(isPresented: Binding<Bool>(
            get: { viewModel.errorMessage != nil },
            set: { _ in viewModel.errorMessage = nil }
        )) {
            Alert(
                title: Text("Error"),
                message: Text(viewModel.errorMessage ?? ""),
                dismissButton: .default(Text("OK")) {
                    viewModel.errorMessage = nil
                }
            )
        }
    }
}
