import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

export interface ToastMessage {
  message: string;
  type: 'success' | 'danger' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private messagesSubject = new Subject<ToastMessage[]>();
  private currentMessages: ToastMessage[] = [];

  messages$: Observable<ToastMessage[]> = this.messagesSubject.asObservable();

  showSuccess(message: string): void {
    this.addMessage(message, 'success');
  }

  showError(message: string): void {
    this.addMessage(message, 'danger');
  }

  showInfo(message: string): void {
    this.addMessage(message, 'info');
  }

  private addMessage(message: string, type: 'success' | 'danger' | 'info'): void {
    const newMessage: ToastMessage = { message, type };
    this.currentMessages.push(newMessage);
    this.messagesSubject.next(this.currentMessages);

    // Remove a mensagem após 5 segundos
    setTimeout(() => {
      this.removeMessage(newMessage);
    }, 5000);
  }

  private removeMessage(messageToRemove: ToastMessage): void {
    this.currentMessages = this.currentMessages.filter(msg => msg !== messageToRemove);
    this.messagesSubject.next(this.currentMessages);
  }

  // Método para limpar todas as mensagens, se necessário
  clear(): void {
    this.currentMessages = [];
    this.messagesSubject.next([]);
  }
}
